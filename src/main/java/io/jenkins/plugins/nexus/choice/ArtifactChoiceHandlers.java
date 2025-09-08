package io.jenkins.plugins.nexus.choice;

import io.jenkins.plugins.nexus.utils.Registry;

public final class ArtifactChoiceHandlers {
    private ArtifactChoiceHandlers() {
    }

    public static ArtifactChoiceHandler getDockerHandler(Registry registry) {
        if (Registry.ECR.equals(registry)) {
            return new ECRArtifactChoiceHandler();
        }
        return new NexusArtifactChoiceHandler();
    }

    public static ArtifactChoiceHandler getHandler() {
        return new NexusArtifactChoiceHandler();
    }


}
