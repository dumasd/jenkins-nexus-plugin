package io.jenkins.plugins.nexus;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.ParameterValue;
import hudson.model.Run;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.DataBoundConstructor;

@Setter
@Getter
public class NexusArtifactChoicesParameterValue extends ParameterValue {

    private final List<String> value;

    @DataBoundConstructor
    public NexusArtifactChoicesParameterValue(String name, List<String> value) {
        super(name);
        this.value = Util.fixNull(value);
    }

    @Override
    public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        String v = String.join(",", value);
        env.put(getName(), v);
    }
}
