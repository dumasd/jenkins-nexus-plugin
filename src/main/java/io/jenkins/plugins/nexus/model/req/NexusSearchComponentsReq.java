package io.jenkins.plugins.nexus.model.req;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Builder
public class NexusSearchComponentsReq implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId;

    private String artifactId;

    private String continuationToken;

    private boolean onlyArtifactId;
}
