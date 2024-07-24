package io.jenkins.plugins.nexus.config;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.nexus.utils.NexusRepositoryClient;
import io.jenkins.plugins.nexus.utils.Utils;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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

    public String getAuthorization() {
        return credentialsIdToAuthorization(credentialsId);
    }

    public static String credentialsIdToAuthorization(String credentialsId) {
        StandardUsernameCredentials credentials = findCredential(credentialsId);
        if (Objects.isNull(credentials)) {
            return null;
        }
        Secret pass = ((StandardUsernamePasswordCredentials) credentials).getPassword();
        String usrPwd = credentials.getUsername() + ":" + Secret.toString(pass);
        return "Basic " + Base64.getEncoder().encodeToString(usrPwd.getBytes(StandardCharsets.UTF_8));
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
                @QueryParameter("docker") boolean docker)
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
            String auth = credentialsIdToAuthorization(credentialsId);
            NexusRepositoryClient client = new NexusRepositoryClient(serverUrl, auth, docker);
            client.check();
            return FormValidation.ok("Validate success");
        }

        public ListBoxModel doFillCredentialsIdItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Select a credential", "");
            for (StandardUsernameCredentials c : CredentialsProvider.lookupCredentialsInItemGroup(
                    StandardUsernameCredentials.class, Jenkins.get(), null, Collections.emptyList())) {
                items.add(c.getId(), c.getId());
            }
            return items;
        }
    }
}
