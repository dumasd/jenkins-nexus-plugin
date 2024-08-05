package io.jenkins.plugins.nexus.model.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Bruce.Wu
 * @date 2024-08-05
 */
@Setter
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Artifact implements Serializable {
    private static final long serialVersionUID = 1L;
    private String groupId;
    private String artifactId;
    private String version;
}
