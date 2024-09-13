package io.jenkins.plugins.nexus.model.dto;

import java.io.File;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder
@ToString
public class NexusDownloadFileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String downloadUrl;

    private File file;
}
