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
import java.io.IOException;
import java.io.Serializable;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Setter
@Getter
public class NexusArtifactDownloader extends Builder implements SimpleBuildStep, Serializable {
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

    @DataBoundConstructor
    public NexusArtifactDownloader(
            String serverId, String repository, String groupId, String artifactId, String version) {
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
        SimpleBuildStep.super.perform(run, workspace, env, launcher, listener);
    }

    @Symbol("nexusArtifactDownload")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
