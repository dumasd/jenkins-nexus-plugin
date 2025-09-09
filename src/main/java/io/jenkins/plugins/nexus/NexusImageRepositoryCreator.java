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
import io.jenkins.plugins.nexus.handler.ArtifactHandler;
import io.jenkins.plugins.nexus.handler.ArtifactHandlers;
import io.jenkins.plugins.nexus.model.dto.CreateImageRepositoryResult;
import io.jenkins.plugins.nexus.utils.Logger;
import io.jenkins.plugins.nexus.utils.Registry;
import io.jenkins.plugins.nexus.utils.Utils;
import java.io.IOException;
import java.io.Serializable;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

@Log
@Getter
public class NexusImageRepositoryCreator extends Builder implements SimpleBuildStep, Serializable {

    public static final String NAME = "NexusCreateImageRepository";

    private final String serverId;

    private final String repository;

    private boolean mutable = true;

    @DataBoundConstructor
    public NexusImageRepositoryCreator(String serverId, String repository) {
        this.serverId = serverId;
        this.repository = repository;
    }

    @DataBoundSetter
    public void setMutable(boolean mutable) {
        this.mutable = mutable;
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
        NexusRepoServerConfig serverConfig =
                NexusRepoServerGlobalConfig.getConfig(serverId).orElseThrow();
        Registry registry = serverConfig.getRegistryEnum();
        ArtifactHandler handler = ArtifactHandlers.getHandler(registry);
        CreateImageRepositoryResult result = handler.createImageRepository(serverConfig, repository, mutable);
        if (result.isExists()) {
            logger.log("Image repository exists: " + repository);
        } else {
            logger.log("Image repository created: " + repository);
        }
    }

    @Extension
    @Symbol("nexusImageRepositoryCreate")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nexus Create Image Repository";
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

        public ListBoxModel doFillServerIdItems() {
            ListBoxModel items = new ListBoxModel();
            NexusRepoServerGlobalConfig.getInstance()
                    .getConfigs()
                    .forEach(e -> items.add(e.getDisplayName(), e.getServerId()));
            return items;
        }
    }
}
