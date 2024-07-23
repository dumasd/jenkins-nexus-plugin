package io.jenkins.plugins.nexus.model.dto;

import java.io.File;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class NexusDownloadFileDTO {

    private String downloadUrl;

    private File file;
}
