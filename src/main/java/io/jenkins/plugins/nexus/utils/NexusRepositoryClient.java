package io.jenkins.plugins.nexus.utils;

import static io.jenkins.plugins.nexus.utils.Constants.DIRECTION;
import static io.jenkins.plugins.nexus.utils.Constants.GROUP;
import static io.jenkins.plugins.nexus.utils.Constants.NAME;
import static io.jenkins.plugins.nexus.utils.Constants.REPOSITORY;
import static io.jenkins.plugins.nexus.utils.Constants.SORT;
import static io.jenkins.plugins.nexus.utils.Constants.VERSION;

import com.alibaba.fastjson2.JSON;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.model.dto.NexusDownloadFileDTO;
import io.jenkins.plugins.nexus.model.req.NexusSearchAssertsReq;
import io.jenkins.plugins.nexus.model.req.NexusSearchComponentsReq;
import io.jenkins.plugins.nexus.model.req.NexusUploadSingleComponentReq;
import io.jenkins.plugins.nexus.model.resp.NexusRepositoryDetails;
import io.jenkins.plugins.nexus.model.resp.NexusSearchAssertsResp;
import io.jenkins.plugins.nexus.model.resp.NexusSearchComponentsResp;
import io.jenkins.plugins.nexus.model.resp.SearchDockerTagsResp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;

/**
 * Nexus客户端
 *
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Getter
@Log
public class NexusRepositoryClient implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String url;

    private final String authorization;

    private final boolean docker;

    private static final int MAX_ASSERTS = 12;

    public NexusRepositoryClient(String url, String authorization, boolean docker) {
        this.url = url;
        this.authorization = authorization;
        this.docker = docker;
    }

    public NexusRepositoryClient(NexusRepoServerConfig cfg) {
        this(cfg.getServerUrl(), cfg.getAuthorization(), cfg.isDocker());
    }

    public NexusRepositoryClient(NexusRepoServerConfig cfg, String authorization) {
        this(cfg.getServerUrl(), authorization, cfg.isDocker());
    }

    /**
     * 检查
     */
    public void check() {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            HttpGet request = new HttpGet(url);
            if (docker) {
                request = new HttpGet(url + "/v2/_catalog");
            }
            if (Utils.isNotEmpty(authorization)) {
                request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
            }
            httpClient.execute(request, new BasicHttpClientResponseHandler());
        } catch (IOException e) {
            throw new NexusClientException(e);
        }
    }

    public NexusRepositoryDetails getRepositoryDetails(String name) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            HttpGet request = new HttpGet(url + "/service/rest/v1/repositories/" + name);
            if (Utils.isNotEmpty(authorization)) {
                request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
            }
            return httpClient.execute(request, new AbstractHttpClientResponseHandler<>() {
                @Override
                public NexusRepositoryDetails handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    return JSON.parseObject(bs, NexusRepositoryDetails.class);
                }
            });
        } catch (IOException e) {
            throw new NexusClientException(e);
        }
    }

    public void uploadSingleComponent(NexusRepositoryDetails nxRepo, NexusUploadSingleComponentReq req) {
        if (!NexusRepositoryType.isSupported(nxRepo.getType())) {
            throw new NexusClientException("Only support hosted type");
        }
        if (!NexusRepositoryFormat.isSupported(nxRepo.getFormat())) {
            throw new NexusClientException("Only support maven2, raw format");
        }
        if (CollectionUtils.isEmpty(req.getFileAsserts())) {
            return;
        }
        NexusRepositoryFormat format = NexusRepositoryFormat.valueOf(nxRepo.getFormat());
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            List<HttpEntity> multipartEntities = new LinkedList<>();
            if (NexusRepositoryFormat.raw.equals(format)) {
                if (req.getFileAsserts().size() > MAX_ASSERTS) {
                    throw new NexusClientException("Upload raw files more than 12");
                }
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                for (int i = 0, len = req.getFileAsserts().size(); i < len; i++) {
                    String prefix = String.format("raw.assert%d", (i % 3) + 1);
                    File file = req.getFileAsserts().get(i).getFile();
                    builder.addPart(prefix, new FileBody(file)).addTextBody(prefix + ".filename", file.getName());
                    if (i + 1 >= len || (i + 1) % 3 == 0) {
                        builder.addTextBody("raw.directory", req.toDirectory());
                        multipartEntities.add(builder.build());
                        builder = MultipartEntityBuilder.create();
                    }
                }
            } else if (NexusRepositoryFormat.maven2.equals(format)) {
                if (req.getFileAsserts().size() > MAX_ASSERTS) {
                    throw new NexusClientException("Upload maven files more than 12");
                }
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                for (int i = 0, len = req.getFileAsserts().size(); i < len; i++) {
                    String prefix = String.format("maven2.assert%d", (i % 3) + 1);
                    NexusUploadSingleComponentReq.FileAssert fileAssert =
                            req.getFileAsserts().get(i);
                    builder.addPart(prefix, new FileBody(fileAssert.getFile()))
                            .addTextBody(
                                    prefix + ".filename", fileAssert.getFile().getName())
                            .addTextBody(prefix + ".extension", fileAssert.fileExt());
                    if (i + 1 >= len || (i + 1) % 3 == 0) {
                        builder.addTextBody("maven2.groupId", req.getGroup());
                        builder.addTextBody("maven2.artifactId", req.getArtifactId());
                        builder.addTextBody("maven2.generate-pom", Boolean.toString(req.isGeneratePom()));
                        if (req.isGeneratePom()) {
                            builder.addTextBody("maven2.packaging", req.getPacking());
                        }
                        multipartEntities.add(builder.build());
                        builder = MultipartEntityBuilder.create();
                    }
                }
            }
            for (HttpEntity entity : multipartEntities) {
                HttpPost request = new HttpPost(url + "/service/rest/v1/components?repository=" + nxRepo.getName());
                if (Utils.isNotEmpty(authorization)) {
                    request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
                }
                request.setEntity(entity);
                httpClient.execute(request, new BasicHttpClientResponseHandler());
            }
        } catch (IOException e) {
            throw new NexusClientException(e);
        }
    }

    public NexusSearchComponentsResp searchComponents(NexusRepositoryDetails nxRepo, NexusSearchComponentsReq req) {
        if (!NexusRepositoryFormat.isSupported(nxRepo.getFormat())) {
            throw new NexusClientException("Only support maven2, raw format");
        }
        NexusRepositoryFormat format = NexusRepositoryFormat.valueOf(nxRepo.getFormat());
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder = new URIBuilder(url + "/service/rest/v1/search")
                    .addParameter(REPOSITORY, nxRepo.getName())
                    .addParameter(DIRECTION, "desc");
            if (NexusRepositoryFormat.raw.equals(format)) {
                String q = Utils.toNexusDictionary(req.getGroupId(), req.getArtifactId());
                uriBuilder.addParameter("q", "\"" + q + "\"").addParameter(SORT, GROUP);
            } else if (NexusRepositoryFormat.maven2.equals(format)) {
                uriBuilder
                        .addParameter(GROUP, req.getGroupId())
                        .addParameter(NAME, req.getArtifactId())
                        .addParameter(SORT, VERSION);
            } else {
                throw new NexusClientException("Only support maven2, raw format");
            }

            if (StringUtils.isNotEmpty(req.getContinuationToken())) {
                uriBuilder.addParameter("continuationToken", req.getContinuationToken());
            }

            log.log(Level.INFO, "Query raw components. repository={0}, uri={1}", new Object[] {
                nxRepo.getName(), uriBuilder.toString()
            });
            HttpGet httpGet = new HttpGet(uriBuilder.toString());
            if (Utils.isNotEmpty(authorization)) {
                httpGet.addHeader(HttpHeaders.AUTHORIZATION, authorization);
            }
            return httpClient.execute(httpGet, new AbstractHttpClientResponseHandler<>() {
                @Override
                public NexusSearchComponentsResp handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseObject(bs, NexusSearchComponentsResp.class);
                }
            });
        } catch (Exception e) {
            throw new NexusClientException(e);
        }
    }

    public NexusSearchAssertsResp searchAsserts(NexusRepositoryDetails nxRepo, NexusSearchAssertsReq req) {
        if (!NexusRepositoryFormat.isSupported(nxRepo.getFormat())) {
            throw new NexusClientException("Only support maven2, raw format");
        }
        NexusRepositoryFormat format = NexusRepositoryFormat.valueOf(nxRepo.getFormat());
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            URIBuilder uriBuilder =
                    new URIBuilder(url + "/service/rest/v1/search/assets").addParameter("repository", nxRepo.getName());
            if (NexusRepositoryFormat.raw.equals(format)) {
                String q = Utils.toNexusDictionary(req.getGroupId(), req.getArtifactId());
                uriBuilder.addParameter(GROUP, q + req.getVersion());
            } else if (NexusRepositoryFormat.maven2.equals(format)) {
                uriBuilder
                        .addParameter(GROUP, req.getGroupId())
                        .addParameter(NAME, req.getArtifactId())
                        .addParameter(VERSION, req.getVersion());
            } else {
                throw new NexusClientException("Only support maven2, raw format");
            }
            HttpGet httpGet = new HttpGet(uriBuilder.toString());
            if (Utils.isNotEmpty(authorization)) {
                httpGet.addHeader(HttpHeaders.AUTHORIZATION, authorization);
            }
            return httpClient.execute(httpGet, new AbstractHttpClientResponseHandler<>() {
                @Override
                public NexusSearchAssertsResp handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseObject(bs, NexusSearchAssertsResp.class);
                }
            });
        } catch (Exception e) {
            throw new NexusClientException(e);
        }
    }

    public void download(List<NexusDownloadFileDTO> dowloadList) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            for (NexusDownloadFileDTO ndf : dowloadList) {
                HttpGet httpGet = new HttpGet(ndf.getDownloadUrl());
                if (Utils.isNotEmpty(authorization)) {
                    httpGet.addHeader(HttpHeaders.AUTHORIZATION, authorization);
                }
                httpClient.execute(httpGet, new AbstractHttpClientResponseHandler<>() {
                    @Override
                    public Object handleEntity(HttpEntity entity) throws IOException {
                        File file = ndf.getFile();
                        File parent = file.getParentFile();
                        if (Objects.nonNull(parent) && !parent.exists() && !parent.mkdirs()) {
                            throw new IOException("Mkdir error");
                        }
                        if (file.exists() && !file.delete()) {
                            throw new IOException("Delete old file fail. file:" + file.getName());
                        }
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            entity.writeTo(fos);
                            fos.flush();
                        }
                        EntityUtils.consume(entity);
                        return true;
                    }
                });
            }
        } catch (IOException e) {
            throw new NexusClientException(e);
        }
    }

    public void deleteComponents(Set<String> componentIds) {
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            for (String id : componentIds) {
                HttpDelete httpDelete = new HttpDelete(url + "/service/rest/v1/components/" + id);
                if (Utils.isNotEmpty(authorization)) {
                    httpDelete.addHeader(HttpHeaders.AUTHORIZATION, authorization);
                }
                httpClient.execute(httpDelete, new BasicHttpClientResponseHandler());
            }
        } catch (IOException e) {
            throw new NexusClientException(e);
        }
    }

    public SearchDockerTagsResp searchDockerTags(NexusSearchComponentsReq req) {
        String imageName = req.getGroupId() + "/" + req.getArtifactId();
        try (CloseableHttpClient httpClient = HttpUtils.createClient()) {
            HttpGet request = new HttpGet(String.format("%s/v2/%s/tags/list", url, imageName));
            if (Utils.isNotEmpty(authorization)) {
                request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
            }
            return httpClient.execute(request, new AbstractHttpClientResponseHandler<SearchDockerTagsResp>() {
                @Override
                public SearchDockerTagsResp handleEntity(HttpEntity entity) throws IOException {
                    byte[] bs = EntityUtils.toByteArray(entity);
                    EntityUtils.consume(entity);
                    return JSON.parseObject(bs, SearchDockerTagsResp.class);
                }
            });
        } catch (IOException e) {
            throw new NexusClientException(e);
        }
    }
}
