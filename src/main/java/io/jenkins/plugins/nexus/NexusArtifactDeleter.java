package io.jenkins.plugins.nexus;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.config.NexusRepoServerGlobalConfig;
import io.jenkins.plugins.nexus.model.req.NexusSearchComponentsReq;
import io.jenkins.plugins.nexus.model.resp.NexusComponentDetails;
import io.jenkins.plugins.nexus.model.resp.NexusRepositoryDetails;
import io.jenkins.plugins.nexus.model.resp.NexusSearchComponentsResp;
import io.jenkins.plugins.nexus.utils.Logger;
import io.jenkins.plugins.nexus.utils.NexusRepositoryClient;
import io.jenkins.plugins.nexus.utils.Utils;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @date 2024-07-19
 */
@Setter
@Getter
public class NexusArtifactDeleter extends Builder implements SimpleBuildStep {

    public static final String NAME = "NexusArtifactDeleter";
    /**
     * Nexus Repo Server ID
     */
    private final String serverId;
    /**
     * Nexus Repository
     */
    private final String repository;
    /**
     * Group
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

    @DataBoundConstructor
    public NexusArtifactDeleter(String serverId, String repository, String groupId, String artifactId, String version) {
        this.serverId = serverId;
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
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
        Logger logger = new Logger(NAME, listener);
        NexusRepoServerConfig nxRepoCfg = nxRepoCfgOp.get();
        NexusRepositoryClient client = new NexusRepositoryClient(nxRepoCfg);
        NexusRepositoryDetails nxRepo = client.getRepositoryDetails(env.expand(repository));
        NexusSearchComponentsReq.NexusSearchComponentsReqBuilder reqBuilder =
                NexusSearchComponentsReq.builder().groupId(groupId).artifactId(artifactId);
        int loopNum = 0;
        String continuationToken = null;
        Set<String> deleteComponentIds = new HashSet<>();
        while (loopNum < 50) {
            reqBuilder.continuationToken(continuationToken);
            NexusSearchComponentsResp resp = client.searchComponents(nxRepo, reqBuilder.build());
            if (CollectionUtils.isEmpty(resp.getItems())) {
                break;
            }
            for (NexusComponentDetails c : resp.getItems()) {
                deleteComponentIds.add(c.getId());
                logger.log("Deleting nexus component. id: %s, name: %s", c.getId(), c.getName());
            }
            if (StringUtils.isBlank(resp.getContinuationToken()) || CollectionUtils.isEmpty(resp.getItems())) {
                break;
            }
            continuationToken = resp.getContinuationToken();
            loopNum++;
        }
        if (CollectionUtils.isNotEmpty(deleteComponentIds)) {
            client.deleteComponents(deleteComponentIds);
        }
    }

    @Extension
    @Symbol("nexusArtifactDelete")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nexus Artifact Deleter";
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

        public ListBoxModel doFillServerIdItems() {
            ListBoxModel items = new ListBoxModel();
            NexusRepoServerGlobalConfig.getInstance()
                    .getConfigs()
                    .forEach(e -> items.add(e.getDisplayName(), e.getServerId()));
            return items;
        }
    }
}
