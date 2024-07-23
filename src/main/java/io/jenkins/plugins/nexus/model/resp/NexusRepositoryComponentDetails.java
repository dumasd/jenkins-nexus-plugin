package io.jenkins.plugins.nexus.model.resp;

import io.jenkins.plugins.nexus.utils.NexusRepositoryFormat;
import io.jenkins.plugins.nexus.utils.Utils;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

/**
 * @author Bruce.Wu
 * @date 2024-07-22
 */
@Setter
@Getter
@ToString
public class NexusRepositoryComponentDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String repository;
    private String format;
    private String group;
    private String name;
    private String version;

    public String version(String groupId, String artifactId) {
        if (StringUtils.isNotBlank(version)) {
            return version;
        }
        if (NexusRepositoryFormat.raw.matches(format)) {
            String prefix = Utils.toNexusDictionary(groupId, artifactId);
            int idx = group.indexOf(prefix);
            if (idx == 0) {
                String s = group.substring(prefix.length());
                if (StringUtils.isNotBlank(s)) {
                    return s;
                }
            }
        }
        return null;
    }
}
