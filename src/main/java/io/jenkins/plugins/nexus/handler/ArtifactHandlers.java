package io.jenkins.plugins.nexus.handler;

import io.jenkins.plugins.nexus.utils.Registry;

public final class ArtifactHandlers {
    private ArtifactHandlers() {}

    public static ArtifactHandler getHandler(Registry registry) {
        if (Registry.ECR.equals(registry)) {
            return new ECRArtifactHandler();
        }
        return new NexusArtifactHandler();
    }
}
