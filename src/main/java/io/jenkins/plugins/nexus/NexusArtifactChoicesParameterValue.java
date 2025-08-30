package io.jenkins.plugins.nexus;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.ParameterValue;
import hudson.model.Run;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.DataBoundConstructor;

@Setter
@Getter
public class NexusArtifactChoicesParameterValue extends ParameterValue {

    private static final long serialVersionUID = 9049224812498272342L;

    private final String value;

    @DataBoundConstructor
    public NexusArtifactChoicesParameterValue(String name, String value) {
        super(name);
        this.value = Util.fixNull(value);
    }

    @Override
    public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        env.put(getName(), value);
    }
}
