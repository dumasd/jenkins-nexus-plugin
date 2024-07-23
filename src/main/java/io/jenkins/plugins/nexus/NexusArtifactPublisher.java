package io.jenkins.plugins.nexus;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.config.NexusRepoServerGlobalConfig;
import io.jenkins.plugins.nexus.model.req.UploadSingleComponentReq;
import io.jenkins.plugins.nexus.model.resp.NexusRepositoryDetails;
import io.jenkins.plugins.nexus.utils.Logger;
import io.jenkins.plugins.nexus.utils.NexusRepositoryClient;
import io.jenkins.plugins.nexus.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.ArrayUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @date 2024-07-19
 */
@Setter
@Getter
public class NexusArtifactPublisher extends Recorder implements SimpleBuildStep, Serializable {

    public static final String NAME = "NexusArtifactPublisher";

    private static final long serialVersionUID = 1L;
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

    private boolean generatePom = false;

    private String packing = "jar";

    private String includes;

    private String excludes;

    @DataBoundConstructor
    public NexusArtifactPublisher(
            String serverId, String repository, String groupId, String artifactId, String version, String includes) {
        this.serverId = serverId;
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.includes = includes;
    }

    @DataBoundSetter
    public void setGeneratePom(boolean generatePom) {
        this.generatePom = generatePom;
    }

    @DataBoundSetter
    public void setPacking(String packing) {
        this.packing = packing;
    }

    @DataBoundSetter
    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath workspace,
            @NonNull EnvVars env,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        Logger logger = new Logger(NAME, listener);
        Optional<NexusRepoServerConfig> nxRepoCfgOp = NexusRepoServerGlobalConfig.getConfig(serverId);
        if (nxRepoCfgOp.isEmpty()) {
            throw new IOException("Nexus repository server not found. serverId=" + serverId);
        }
        NexusRepoServerConfig nxRepoCfg = nxRepoCfgOp.get();
        NexusRepositoryClient client =
                new NexusRepositoryClient(nxRepoCfg.getServerUrl(), nxRepoCfg.getAuthorization());
        NexusRepositoryDetails nxRepo = client.getRepositoryDetails(env.expand(repository));
        UploadSingleComponentReq req = new UploadSingleComponentReq();
        req.setGroup(env.expand(groupId));
        req.setArtifactId(env.expand(artifactId));
        req.setPacking(env.expand(packing));
        req.setVersion(env.expand(version));
        req.setGeneratePom(generatePom);
        List<UploadSingleComponentReq.FileAssert> fileAsserts = new LinkedList<>();
        FilePath[] paths = workspace.list(env.expand(includes), env.expand(excludes));
        if (ArrayUtils.isEmpty(paths)) {
            logger.log("There is no file to upload!!!!!!");
            return;
        }
        for (FilePath fp : paths) {
            fileAsserts.add(new UploadSingleComponentReq.FileAssert(new File(fp.getRemote())));
        }
        req.setFileAsserts(fileAsserts);
        workspace.act(new UploadFileCallable(listener, client, nxRepo, req));
    }

    public static class UploadFileCallable implements Callable<Boolean, IOException> {

        private final TaskListener listener;
        private final NexusRepositoryClient client;
        private final NexusRepositoryDetails repositoryDetails;
        private final UploadSingleComponentReq uploadSingleComponentReq;

        public UploadFileCallable(
                TaskListener listener,
                NexusRepositoryClient client,
                NexusRepositoryDetails repositoryDetails,
                UploadSingleComponentReq uploadSingleComponentReq) {
            this.listener = listener;
            this.client = client;
            this.repositoryDetails = repositoryDetails;
            this.uploadSingleComponentReq = uploadSingleComponentReq;
        }

        @Override
        public Boolean call() throws IOException {
            Logger logger = new Logger(NAME, listener);
            logger.log(
                    "Publishing to repository. name=%s, format=%s",
                    repositoryDetails.getName(), repositoryDetails.getFormat());
            logger.log(
                    "Publishing files. group=%s, artifactId=%s, version=%s, packing=%s ::::: \n%s",
                    uploadSingleComponentReq.getGroup(),
                    uploadSingleComponentReq.getArtifactId(),
                    uploadSingleComponentReq.getVersion(),
                    uploadSingleComponentReq.getPacking(),
                    uploadSingleComponentReq.assertsPrintInfo());
            client.uploadSingleComponent(repositoryDetails, uploadSingleComponentReq);
            return true;
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {}
    }

    @Extension
    @Symbol("nexusArtifactPublish")
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

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
        public FormValidation doCheckIncludes(@QueryParameter("includes") String value) {
            if (Utils.isNullOrEmpty(value)) {
                return FormValidation.error("Includes is required");
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
