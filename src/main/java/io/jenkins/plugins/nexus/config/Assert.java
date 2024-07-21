package io.jenkins.plugins.nexus.config;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.nexus.utils.Utils;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author Bruce.Wu
 * @date 2024-07-20
 */
@Setter
@Getter
@ToString
public class Assert extends AbstractDescribableImpl<Assert> implements Serializable {
    private static final long serialVersionUID = 2905162041950251407L;

    private String file;

    private String classifier;

    private String extension;

    public Assert(String file) {
        this.file = file;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Assert> {
        public FormValidation doCheckArtifactId(@QueryParameter String value) {
            if (Utils.isNullOrEmpty(value)) {
                return FormValidation.error("ArtifactId must not be empty");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckFile(@QueryParameter String value) {
            if (Utils.isNullOrEmpty(value)) {
                return FormValidation.error("File must not be empty");
            }
            return FormValidation.ok();
        }
    }
}
