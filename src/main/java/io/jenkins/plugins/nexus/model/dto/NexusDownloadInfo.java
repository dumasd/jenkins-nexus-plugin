package io.jenkins.plugins.nexus.model.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class NexusDownloadInfo implements Serializable {
    private static final long serialVersionUID = -20281355083246331L;

    private String downloadUrl;

    private String filePath;

    public NexusDownloadInfo() {}

    public NexusDownloadInfo(String downloadUrl, String filePath) {
        this.downloadUrl = downloadUrl;
        this.filePath = filePath;
    }
}
