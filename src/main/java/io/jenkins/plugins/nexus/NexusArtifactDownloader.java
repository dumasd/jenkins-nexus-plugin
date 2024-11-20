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
import io.jenkins.plugins.nexus.model.dto.NexusArtifactDownloadResult;
import io.jenkins.plugins.nexus.model.dto.NexusDownloadInfo;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import jenkins.MasterToSlaveFileCallable;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @since 2024-07-20
 */
@Log
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
    private Integer maxAssertNum;

    @DataBoundConstructor
    public NexusArtifactDownloader(
            @NonNull String serverId,
            @NonNull String repository,
            @NonNull String groupId,
            @NonNull String artifactId,
            @NonNull String version,
            Integer maxAssertNum) {
        this.serverId = serverId;
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.maxAssertNum = Objects.requireNonNullElse(maxAssertNum, 50);
    }

    @DataBoundSetter
    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath workspace,
            @NonNull EnvVars env,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        Logger logger = new Logger(NexusArtifactDownloader.NAME, listener);
        NexusRepoServerConfig nxRepoCfg =
                NexusRepoServerGlobalConfig.getConfig(serverId).orElseThrow();

        String locationEx = env.expand(location);
        String repositoryEx = env.expand(repository);
        String groupIdEx = env.expand(groupId);
        String artifactIdEx = env.expand(artifactId);
        String versionEx = env.expand(version);

        FilePath target = workspace;
        if (Utils.isNotEmpty(locationEx)) {
            target = workspace.child(locationEx);
        }

        logger.log(
                "Download info. serverUrl: %s, repository: %s, groupId: %s, artifactId: %s, version: %s, location: %s",
                nxRepoCfg.getServerUrl(), repositoryEx, groupIdEx, artifactIdEx, versionEx, locationEx);

        String auth = nxRepoCfg.getAuthorization();

        long startTime = System.currentTimeMillis();
        NexusRepositoryClient client = new NexusRepositoryClient(nxRepoCfg, auth);
        NexusRepositoryDetails nxRepo = client.getRepositoryDetails(repositoryEx);
        log.log(Level.INFO, "Get nexus repository details spend time: {0}", (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();

        List<NexusDownloadInfo> downloadInfos = new ArrayList<>();
        if (Utils.isNotEmpty(locationEx) && Utils.isFile(locationEx)) {
            String downloadUrl = String.format(
                    "%s%s%s/%s",
                    nxRepo.getUrl(), Utils.toNexusDictionary(groupIdEx, artifactIdEx), versionEx, target.getName());
            NexusDownloadInfo di = new NexusDownloadInfo(downloadUrl, target.getRemote());
            downloadInfos.add(di);
        } else {
            String continuationToken = null;
            Set<NexusAssertDetails> asserts = new LinkedHashSet<>();

            while (asserts.size() < maxAssertNum) {
                NexusSearchAssertsReq.NexusSearchAssertsReqBuilder reqBuilder = NexusSearchAssertsReq.builder()
                        .groupId(groupIdEx)
                        .artifactId(artifactIdEx)
                        .version(versionEx)
                        .continuationToken(continuationToken);
                NexusSearchAssertsResp resp = client.searchAsserts(nxRepo, reqBuilder.build());
                asserts.addAll(resp.getItems());

                if (StringUtils.isBlank(resp.getContinuationToken())) {
                    break;
                }
                continuationToken = resp.getContinuationToken();
            }

            for (NexusAssertDetails ass : asserts) {
                String fileName = Utils.getFileName(ass.getPath());
                FilePath dfp = target.child(fileName);
                NexusDownloadInfo di = new NexusDownloadInfo(ass.getDownloadUrl(), dfp.getRemote());
                downloadInfos.add(di);
            }
            log.log(Level.INFO, "Search asserts spend time: {0}ms", (System.currentTimeMillis() - startTime));
        }

        if (downloadInfos.isEmpty()) {
            logger.log("Not found file to download!!!");
        } else {
            NexusArtifactDownloadResult result = target.act(new RemoteDownloader(auth, nxRepoCfg, downloadInfos));
            for (NexusDownloadInfo df : downloadInfos) {
                logger.log("Download file: %s", df.getFilePath());
            }
            logger.log("Download spend time: %sms", String.valueOf(result.getSpendTime()));
            log.log(Level.INFO, "Download spend time: {0}ms", result.getSpendTime());
        }
    }

    private static class RemoteDownloader extends MasterToSlaveFileCallable<NexusArtifactDownloadResult> {
        private static final java.util.logging.Logger LOGGER =
                java.util.logging.Logger.getLogger(NexusArtifactDownloader.RemoteDownloader.class.getName());
        private static final long serialVersionUID = 6854543502604967968L;

        private final String auth;
        private final NexusRepoServerConfig nxRepoCfg;
        private final List<NexusDownloadInfo> downloadInfos;

        public RemoteDownloader(String auth, NexusRepoServerConfig nxRepoCfg, List<NexusDownloadInfo> downloadInfos) {
            this.auth = auth;
            this.nxRepoCfg = nxRepoCfg;
            this.downloadInfos = downloadInfos;
        }

        @Override
        public NexusArtifactDownloadResult invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();
            NexusRepositoryClient client = new NexusRepositoryClient(nxRepoCfg, auth);
            client.downloadFiles(downloadInfos);

            NexusArtifactDownloadResult result = new NexusArtifactDownloadResult();
            result.setSpendTime(System.currentTimeMillis() - startTime);

            LOGGER.log(Level.INFO, "Download asserts spend time: {0}", (System.currentTimeMillis() - startTime));
            return result;
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
