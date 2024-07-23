package io.jenkins.plugins.nexus;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.config.NexusRepoServerGlobalConfig;
import io.jenkins.plugins.nexus.model.dto.NexusDownloadFileDTO;
import io.jenkins.plugins.nexus.model.req.NexusSearchAssertsReq;
import io.jenkins.plugins.nexus.model.resp.NexusAssertDetails;
import io.jenkins.plugins.nexus.model.resp.NexusRepositoryDetails;
import io.jenkins.plugins.nexus.model.resp.NexusSearchAssertsResp;
import io.jenkins.plugins.nexus.utils.Logger;
import io.jenkins.plugins.nexus.utils.NexusRepositoryClient;
import io.jenkins.plugins.nexus.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import jenkins.MasterToSlaveFileCallable;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Setter
@Getter
public class NexusArtifactDownloader extends Builder implements SimpleBuildStep, Serializable {
    private static final long serialVersionUID = 1L;

    public static final String NAME = "NexusArtifactDownloader";

    /**
     * Nexus Repo Server ID
     */
    private final String serverId;
    /**
     * Nexus Repository
     */
    private final String repository;
    /**
     * GroupID
     */
    private final String groupId;
    /**
     * 制品ID
     */
    private final String artifactId;
    /**
     * 制品Version
     */
    private final String version;
    /**
     * 下载位置
     */
    private String location;
    /**
     * 可下载的最大Assert数量
     */
    private int maxAssertNum = 20;

    @DataBoundConstructor
    public NexusArtifactDownloader(
            String serverId, String repository, String groupId, String artifactId, String version) {
        this.serverId = serverId;
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @DataBoundSetter
    public void setLocation(String location) {
        this.location = location;
    }

    @DataBoundSetter
    public void setMaxAssertNum(int maxAssertNum) {
        this.maxAssertNum = maxAssertNum;
    }

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath workspace,
            @NonNull EnvVars env,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        Optional<NexusRepoServerConfig> nxRepoCfgOp = NexusRepoServerGlobalConfig.getConfig(serverId);
        if (nxRepoCfgOp.isEmpty()) {
            throw new IOException("Nexus repository server not found. serverId=" + serverId);
        }
        FilePath target = workspace;
        String locationEx = env.expand(location);
        if (Utils.isNotEmpty(locationEx)) {
            target = workspace.child(locationEx);
        }
        target.act(new RemoteDownloader(
                listener,
                locationEx,
                nxRepoCfgOp.get(),
                env.expand(repository),
                env.expand(groupId),
                env.expand(artifactId),
                env.expand(version),
                maxAssertNum));
    }

    private static class RemoteDownloader extends MasterToSlaveFileCallable<Void> {
        private final TaskListener listener;
        private final String location;
        private final NexusRepoServerConfig nxRepoCfg;
        private final String repository;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final int maxAssertNum;

        private RemoteDownloader(
                TaskListener listener,
                String location,
                NexusRepoServerConfig nxRepoCfg,
                String repository,
                String groupId,
                String artifactId,
                String version,
                int maxAssertNum) {
            this.listener = listener;
            this.location = location;
            this.nxRepoCfg = nxRepoCfg;
            this.repository = repository;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.maxAssertNum = maxAssertNum;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            Logger logger = new Logger(NexusArtifactDownloader.NAME, listener);
            logger.log(
                    "Downloading from repository. serverUrl: %s, repository: %s, groupId: %s, artifactId: %s, version: %s",
                    nxRepoCfg.getServerUrl(), repository, groupId, artifactId, version);
            NexusRepositoryClient client = new NexusRepositoryClient(nxRepoCfg);
            NexusRepositoryDetails nxRepo = client.getRepositoryDetails(repository);
            NexusSearchAssertsReq.NexusSearchAssertsReqBuilder reqBuilder = NexusSearchAssertsReq.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .version(version);
            Set<NexusAssertDetails> asserts = new LinkedHashSet<>();
            String continuationToken = null;
            while (asserts.size() < maxAssertNum) {
                reqBuilder.continuationToken(continuationToken);
                NexusSearchAssertsResp resp = client.searchAsserts(nxRepo, reqBuilder.build());
                asserts.addAll(resp.getItems());
                if (StringUtils.isBlank(resp.getContinuationToken())) {
                    break;
                }
                continuationToken = resp.getContinuationToken();
            }
            if (asserts.isEmpty()) {
                logger.log("There are no files to download !!!!!!");
                return null;
            }
            List<NexusDownloadFileDTO> downloadFiles = new LinkedList<>();
            if (Utils.isNotEmpty(location) && Utils.isFile(location)) {
                String fileName = Utils.getFileName(location);
                NexusDownloadFileDTO downFile = null;
                for (NexusAssertDetails ass : asserts) {
                    if (Objects.equals(fileName, Utils.getFileName(ass.getPath()))) {
                        downFile = NexusDownloadFileDTO.builder()
                                .downloadUrl(ass.getDownloadUrl())
                                .file(f)
                                .build();
                        downloadFiles.add(downFile);
                        break;
                    }
                }
                if (Objects.isNull(downFile)) {
                    logger.log("File not found in nexus repository. fileName: %s", fileName);
                    return null;
                }
            } else {
                for (NexusAssertDetails ass : asserts) {
                    String fileName = Utils.getFileName(ass.getPath());
                    NexusDownloadFileDTO downFile = NexusDownloadFileDTO.builder()
                            .downloadUrl(ass.getDownloadUrl())
                            .file(new File(f, fileName))
                            .build();
                    logger.log(
                            "Downloading file. downloadFile: %s, filePath: %s",
                            downFile.getDownloadUrl(), downFile.getFile());
                    downloadFiles.add(downFile);
                }
            }
            client.download(downloadFiles);
            return null;
        }
    }

    @Symbol("nexusArtifactDownload")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nexus Artifact Downloader";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @POST
        public FormValidation doCheckRepository(@QueryParameter("repository") String value) {
            if (Utils.isNullOrEmpty(value)) {
                return FormValidation.error("Nexus Repository is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckGroup(@QueryParameter("group") String value) {
            if (Utils.isNullOrEmpty(value)) {
                return FormValidation.error("Group is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckArtifactId(@QueryParameter("artifactId") String value) {
            if (Utils.isNullOrEmpty(value)) {
                return FormValidation.error("ArtifactId is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckVersion(@QueryParameter("version") String value) {
            if (Utils.isNullOrEmpty(value)) {
                return FormValidation.error("Version is required");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckMaxAssertNum(@QueryParameter String value) {
            if (Utils.isNotEmpty(value)) {
                try {
                    int num = Integer.parseInt(value);
                    if (num <= 0) {
                        return FormValidation.error("Max assert count must greater than 0");
                    }
                } catch (NumberFormatException e) {
                    return FormValidation.error("Max assert count must be a number");
                }
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillServerIdItems() {
            ListBoxModel items = new ListBoxModel();
            NexusRepoServerGlobalConfig.getInstance()
                    .getConfigs()
                    .forEach(e -> items.add(e.getDisplayName(), e.getServerId()));
            return items;
        }
    }
}
