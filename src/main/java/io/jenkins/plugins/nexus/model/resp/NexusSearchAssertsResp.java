package io.jenkins.plugins.nexus.model.resp;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NexusSearchAssertsResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<NexusAssertDetails> items;

    private String continuationToken;
}
