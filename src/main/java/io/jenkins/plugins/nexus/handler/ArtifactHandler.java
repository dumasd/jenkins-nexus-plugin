package io.jenkins.plugins.nexus.handler;

import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.model.dto.CreateImageRepositoryResult;
import io.jenkins.plugins.nexus.model.dto.GetLoginPasswordResult;

public interface ArtifactHandler {

    ListBoxModel getItems(NexusRepoServerConfig serverConfig, String option, String repository, int limits);

    GetLoginPasswordResult getLoginPassword(NexusRepoServerConfig serverConfig);

    default CreateImageRepositoryResult createImageRepository(
            NexusRepoServerConfig serverConfig, String repo, boolean mutable) {
        CreateImageRepositoryResult result = new CreateImageRepositoryResult();
        result.setExists(true);
        return result;
    }
}
