package io.jenkins.plugins.nexus;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.config.NexusRepoServerGlobalConfig;
import io.jenkins.plugins.nexus.handler.ArtifactHandler;
import io.jenkins.plugins.nexus.handler.ArtifactHandlers;
import io.jenkins.plugins.nexus.model.dto.GetLoginPasswordResult;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.extern.java.Log;
import org.jenkinsci.plugins.workflow.steps.*;

@Log
@Getter
public class NexusDockerLoginStep extends Step {

    private final String serverId;

    public NexusDockerLoginStep(String serverId) {
        this.serverId = serverId;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new StepExecutionImpl(context, serverId);
    }

    public static class StepExecutionImpl extends SynchronousNonBlockingStepExecution<Map<String, String>> {

        private final String serverId;

        public StepExecutionImpl(@NonNull StepContext context, String serverId) {
            super(context);
            this.serverId = serverId;
        }

        @Override
        protected Map<String, String> run() throws Exception {
            // FilePath workspace = getContext().get(FilePath.class);
            // TaskListener taskListener = getContext().get(TaskListener.class);
            // Logger logger = new Logger("NexusDockerLogin", taskListener);

            NexusRepoServerConfig serverConfig =
                    NexusRepoServerGlobalConfig.getConfig(serverId).orElseThrow();

            ArtifactHandler artifactHandler = ArtifactHandlers.getHandler(serverConfig.getRegistryEnum());
            GetLoginPasswordResult getLoginPasswordResult = artifactHandler.getLoginPassword(serverConfig);

            Map<String, String> result = new LinkedHashMap<>();
            result.put("username", getLoginPasswordResult.getUsername());
            result.put("password", getLoginPasswordResult.getPassword());
            result.put("repositoryUri", getLoginPasswordResult.getRepositoryUri());
            return result;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> classes = new HashSet<>();
            classes.add(Run.class);
            classes.add(TaskListener.class);
            classes.add(FilePath.class);
            return classes;
        }

        @Override
        public String getFunctionName() {
            return "nexusDockerLogin";
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
