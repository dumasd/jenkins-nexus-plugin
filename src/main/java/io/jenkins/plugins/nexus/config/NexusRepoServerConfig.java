package io.jenkins.plugins.nexus.config;

import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.nexus.utils.HttpUtils;
import io.jenkins.plugins.nexus.utils.NexusRepositoryClient;
import io.jenkins.plugins.nexus.utils.Registry;
import io.jenkins.plugins.nexus.utils.Utils;
import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import jenkins.model.Jenkins;
import lombok.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Bruce.Wu
 * @date 2024-07-19
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class NexusRepoServerConfig extends AbstractDescribableImpl<NexusRepoServerConfig> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String displayName;
    private String serverId;
    private String serverUrl;
    private String credentialsId;
    private boolean docker;
    private String registry = Registry.NEXUS.name();
    private String region;

    @DataBoundConstructor
    public NexusRepoServerConfig(String displayName, String serverId, String serverUrl) {
        this.displayName = displayName;
        this.serverId = serverId;
        this.serverUrl = serverUrl;
    }

    @DataBoundSetter
    public void setDocker(boolean docker) {
        this.docker = docker;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setRegistry(String registry) {
        Registry.valueOf(registry);
        this.registry = registry;
    }

    @DataBoundSetter
    public void setRegion(String region) {
        this.region = region;
    }

    public String getAuthorization() {
        return credentialsIdToAuthorization(credentialsId);
    }

    public static String credentialsIdToAuthorization(String credentialsId) {
        StandardUsernameCredentials credentials = findCredential(credentialsId);
        if (Objects.isNull(credentials)) {
            return null;
        }
        Secret pass = ((StandardUsernamePasswordCredentials) credentials).getPassword();
        return HttpUtils.getBasicAuth(credentials.getUsername(), Secret.toString(pass));
    }

    public static StandardUsernameCredentials findCredential(String credentialsId) {
        StandardUsernameCredentials credentials = null;
        if (Utils.isNullOrEmpty(credentialsId)) {
            return credentials;
        }

        for (StandardUsernameCredentials c : CredentialsProvider.lookupCredentialsInItemGroup(
                StandardUsernameCredentials.class, Jenkins.get(), null, Collections.emptyList())) {
            if (Objects.equals(c.getId(), credentialsId)) {
                credentials = c;
                break;
            }
        }

        return credentials;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<NexusRepoServerConfig> {

        public FormValidation doTest(
                @QueryParameter("displayName") String displayName,
                @QueryParameter("serverId") String serverId,
                @QueryParameter("serverUrl") String serverUrl,
                @QueryParameter("credentialsId") String credentialsId,
                @QueryParameter("docker") boolean docker,
                @QueryParameter("registry") String registry,
                @QueryParameter("region") String region)
                throws Exception {
            if (Utils.isNullOrEmpty(displayName)) {
                return FormValidation.error("Please input Display Name");
            }
            if (Utils.isNullOrEmpty(serverId)) {
                return FormValidation.error("Please input Server ID");
            }
            if (Utils.isNullOrEmpty(serverUrl)) {
                return FormValidation.error("Please input Server URL");
            }
            if (!(serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))) {
                return FormValidation.error("Invalid Server URL pattern");
            }

            if (docker) {
                Registry registryEnum = Registry.NEXUS;
                if (!Utils.isNullOrEmpty(registry)) {
                    try {
                        registryEnum = Registry.valueOf(registry);
                    } catch (IllegalArgumentException e) {
                        return FormValidation.error("Invalid docker registry", e);
                    }
                }
                if (Registry.NEXUS.equals(registryEnum)) {
                    String auth = credentialsIdToAuthorization(credentialsId);
                    NexusRepositoryClient client = new NexusRepositoryClient(serverUrl, auth, true);
                    client.check();
                } else if (Registry.ECR.equals(registryEnum)) {
                    if (Utils.isNullOrEmpty(region)) {
                        return FormValidation.error("Please input region when registry is ECR");
                    }
                }
            } else {
                String auth = credentialsIdToAuthorization(credentialsId);
                NexusRepositoryClient client = new NexusRepositoryClient(serverUrl, auth, false);
                client.check();
            }

            return FormValidation.ok("Validate success");
        }

        public FormValidation doCheckRegistry(@QueryParameter String value) {
            if (Utils.isNullOrEmpty(value)) {
                return FormValidation.ok();
            }
            try {
                Registry.valueOf(value);
            } catch (IllegalArgumentException e) {
                return FormValidation.error("Invalid docker registry", e);
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(@QueryParameter("registry") String registry) {
            Registry registryEnum = Registry.NEXUS;
            if (!Utils.isNullOrEmpty(registry)) {
                registryEnum = Registry.valueOf(registry);
            }

            ListBoxModel items = new ListBoxModel();
            items.add("Select a credential", "");

            for (StandardUsernameCredentials c : CredentialsProvider.lookupCredentialsInItemGroup(
                    StandardUsernameCredentials.class, Jenkins.get(), null, Collections.emptyList())) {
                items.add(c.getId(), c.getId());
            }

            if (Registry.ECR.equals(registryEnum)) {
                for (AmazonWebServicesCredentials c : CredentialsProvider.lookupCredentialsInItemGroup(
                        AmazonWebServicesCredentials.class, Jenkins.get(), null, Collections.emptyList())) {
                    items.add(c.getDisplayName(), c.getId());
                }
            }
            return items;
        }

        public ListBoxModel doFillRegistryItems() {
            ListBoxModel items = new ListBoxModel();
            for (Registry registry : Registry.values()) {
                items.add(registry.name(), registry.name());
            }
            return items;
        }
    }
}
