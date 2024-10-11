package io.jenkins.plugins.nexus.model.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class NexusArtifactDownloadResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> downloadFiles;

    private long spendTime;
}
