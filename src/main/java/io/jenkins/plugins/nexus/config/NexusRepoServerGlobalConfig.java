package io.jenkins.plugins.nexus.config;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Extension
@Setter
@Getter
@ToString
public class NexusRepoServerGlobalConfig extends Descriptor<NexusRepoServerGlobalConfig>
        implements Describable<NexusRepoServerGlobalConfig>, Serializable {
    private static final long serialVersionUID = 1L;

    private List<NexusRepoServerConfig> configs;

    public NexusRepoServerGlobalConfig() {
        super(NexusRepoServerGlobalConfig.class);
        load();
    }

    @Override
    public Descriptor<NexusRepoServerGlobalConfig> getDescriptor() {
        return Jenkins.get().getDescriptorByType(NexusRepoServerGlobalConfig.class);
    }

    @DataBoundSetter
    public void setConfigs(List<NexusRepoServerConfig> configs) {
        this.configs = configs;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return super.configure(req, json);
    }

    public Descriptor<NexusRepoServerConfig> getConfigDescriptor() {
        return Jenkins.get().getDescriptorByType(NexusRepoServerConfig.DescriptorImpl.class);
    }

    public static NexusRepoServerGlobalConfig getInstance() {
        return Jenkins.get().getDescriptorByType(NexusRepoServerGlobalConfig.class);
    }

    public static Optional<NexusRepoServerConfig> getConfig(String serverId) {
        return getInstance().configs.stream()
                .filter(item -> Objects.equals(serverId, item.getServerId()))
                .findAny();
    }
}
