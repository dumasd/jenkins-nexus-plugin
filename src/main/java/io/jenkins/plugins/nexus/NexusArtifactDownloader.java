package io.jenkins.plugins.nexus;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import java.io.IOException;
import java.io.Serializable;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Setter
@Getter
public class NexusArtifactDownloader extends Builder implements SimpleBuildStep, Serializable {
    private static final long serialVersionUID = 1L;

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
}
