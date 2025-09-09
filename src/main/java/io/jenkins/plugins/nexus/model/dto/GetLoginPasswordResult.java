package io.jenkins.plugins.nexus.model.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GetLoginPasswordResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String repositoryUri;
    private String username;
    private String password;

    public GetLoginPasswordResult() {}

    public GetLoginPasswordResult(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
