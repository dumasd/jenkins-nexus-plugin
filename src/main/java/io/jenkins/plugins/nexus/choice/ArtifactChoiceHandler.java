package io.jenkins.plugins.nexus.choice;

import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;

public interface ArtifactChoiceHandler {

    ListBoxModel getItems(NexusRepoServerConfig serverConfig, String option, String repository, int limits);

}
