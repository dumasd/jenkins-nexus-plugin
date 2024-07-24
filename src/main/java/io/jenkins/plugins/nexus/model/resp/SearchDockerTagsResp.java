package io.jenkins.plugins.nexus.model.resp;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-07-24
 */
@Getter
@Setter
@ToString
public class SearchDockerTagsResp implements Serializable {

    /**
     * image name
     */
    private String name;
    /**
     * tag列表
     */
    private List<String> tags;
}
