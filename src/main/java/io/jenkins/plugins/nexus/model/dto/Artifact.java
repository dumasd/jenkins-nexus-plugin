package io.jenkins.plugins.nexus.model.dto;

import java.io.Serializable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-08-05
 */
@Setter
@Getter
@EqualsAndHashCode
public class Artifact implements Serializable {
    private static final long serialVersionUID = 1L;
    private String groupId;
    private String artifactId;
    private String version;
    private List<Assert> asserts;

    public Artifact(String groupId, String artifactId, String version, List<Assert> asserts) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.asserts = asserts;
    }

    @Setter
    @Getter
    @ToString
    public static class Assert implements Serializable {

        private static final long serialVersionUID = 4320611135852488067L;

        private String name;
        private String link;

        public Assert() {}

        public Assert(String name, String link) {
            this.name = name;
            this.link = link;
        }
    }
}
