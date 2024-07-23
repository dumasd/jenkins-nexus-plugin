package io.jenkins.plugins.nexus.model.resp;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Bruce.Wu
 * @date 2024-07-22
 */
@Setter
@Getter
@ToString
public class NexusSearchComponentsResp implements Serializable {

    private List<NexusRepositoryComponentDetails> items;

    private String continuationToken;
}
