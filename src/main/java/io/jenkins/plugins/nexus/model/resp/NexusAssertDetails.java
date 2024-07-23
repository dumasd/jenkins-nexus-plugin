package io.jenkins.plugins.nexus.model.resp;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-07-23
 */
@Getter
@Setter
@ToString
public class NexusAssertDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String path;
    private String downloadUrl;
    private String repository;
    private String format;
    private String contentType;
    private String lastModified;
    private Long fileSize;
}
