package io.jenkins.plugins.nexus.action;

import io.jenkins.plugins.nexus.model.dto.Artifact;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-08-05
 */
@Getter
@Setter
@ToString
public class NexusArtifactPublisherAction extends AbstractNexusArtifactAction implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<Artifact> artifacts;

    public NexusArtifactPublisherAction() {
        this.artifacts = new LinkedHashSet<>();
    }

    public void addArtifact(String groupId, String artifactId, String version) {
        artifacts.add(new Artifact(groupId, artifactId, version));
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
