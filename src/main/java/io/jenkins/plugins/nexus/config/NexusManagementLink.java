package io.jenkins.plugins.nexus.config;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.util.FormApply;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Extension(ordinal = Double.MAX_VALUE)
public class NexusManagementLink extends ManagementLink {

    @Override
    public String getIconFileName() {
        return "/plugin/jenkins-nexus-plugin/images/sonatype-repository-icon.svg";
    }

    @Override
    public String getDisplayName() {
        return "Nexus";
    }

    @Override
    public String getUrlName() {
        return "nexus";
    }

    @POST
    public void doConfigure(StaplerRequest req, StaplerResponse res)
            throws ServletException, Descriptor.FormException, IOException {
        if (Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            getGlobalConfigDescriptor().configure(req, req.getSubmittedForm());
            FormApply.success(req.getContextPath() + "/manage").generateResponse(req, res, null);
        }
    }

    public Descriptor<NexusRepoServerGlobalConfig> getGlobalConfigDescriptor() {
        return Jenkins.get().getDescriptorByType(NexusRepoServerGlobalConfig.class);
    }
}
