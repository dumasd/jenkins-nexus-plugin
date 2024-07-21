package io.jenkins.plugins.nexus.model.resp;

import java.io.Serializable;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Setter
@Getter
@ToString
public class NexusRepositoryDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String format;
    private String url;
    private String type;
    private Map<String, Object> attributes;
}
