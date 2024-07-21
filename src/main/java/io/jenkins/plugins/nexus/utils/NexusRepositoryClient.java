package io.jenkins.plugins.nexus.utils;

import com.alibaba.fastjson2.JSON;
import io.jenkins.plugins.nexus.model.req.UploadSingleComponentReq;
import io.jenkins.plugins.nexus.model.resp.NexusRepositoryDetails;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * Nexus客户端
 *
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Setter
@Getter
public class NexusRepositoryClient implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String url;

    private final String authorization;

    private static final int MAX_ASSERTS = 12;

    public NexusRepositoryClient(String url, String authorization) {
        this.url = url;
        this.authorization = authorization;
    }

    private static CloseableHttpClient buildHttpClient() {
        try {
            return HttpClientBuilder.create()
                    .setRedirectStrategy(DefaultRedirectStrategy.INSTANCE)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("HttpClient构建失败", e);
        }
    }

    /**
     * 检查
     */
    public void check() {
        try (CloseableHttpClient httpClient = buildHttpClient()) {
            HttpGet request = new HttpGet(url);
            if (Utils.isNotEmpty(authorization)) {
                request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
            }
            httpClient.execute(request, new BasicHttpClientResponseHandler());
        } catch (IOException e) {
            throw new NexusClientException(e);
        }
    }

    public NexusRepositoryDetails getRepositoryDetails(String name) {
        try (CloseableHttpClient httpClient = buildHttpClient()) {
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

    public void uploadSingleComponent(NexusRepositoryDetails nxRepo, UploadSingleComponentReq req) {
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
        try (CloseableHttpClient httpClient = buildHttpClient()) {
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
                    UploadSingleComponentReq.FileAssert fileAssert =
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
}
