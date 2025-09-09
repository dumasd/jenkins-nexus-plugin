package io.jenkins.plugins.nexus.model.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateImageRepositoryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean exists;
}
