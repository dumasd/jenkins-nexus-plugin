package io.jenkins.plugins.nexus;

import static hudson.model.ChoiceParameterDefinition.CHOICES_DELIMITER;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.config.NexusRepoServerGlobalConfig;
import io.jenkins.plugins.nexus.model.req.NexusSearchComponentsReq;
import io.jenkins.plugins.nexus.model.resp.NexusComponentDetails;
import io.jenkins.plugins.nexus.model.resp.NexusRepositoryDetails;
import io.jenkins.plugins.nexus.model.resp.NexusSearchComponentsResp;
import io.jenkins.plugins.nexus.model.resp.SearchDockerTagsResp;
import io.jenkins.plugins.nexus.utils.NexusRepositoryClient;
import io.jenkins.plugins.nexus.utils.NexusRepositoryFormat;
import io.jenkins.plugins.nexus.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Bruce.Wu
 * @date 2024-07-22
 */
@Setter
@Getter
@Log
public class NexusArtifactChoicesParameterDefinition extends ParameterDefinition {

    /**
     * Nexus Server ID
     */
    private String serverId;
    /**
     * 仓库
     */
    private String repository;
    /**
     * 组ID列表
     */
    private String groupIdArtifactIds;
    /**
     * 可见的条目列表
     */
    private int visibleItemCount = 5;
    /**
     * 最大的版本号查找数量
     */
    private int maxVersionCount = 50;

    @DataBoundConstructor
    public NexusArtifactChoicesParameterDefinition(@NonNull String name, String serverId, String repository) {
        super(name);
        this.serverId = serverId;
        this.repository = repository;
    }

    @DataBoundSetter
    public void setGroupIdArtifactIds(String groupIdArtifactIds) {
        this.groupIdArtifactIds = groupIdArtifactIds;
    }

    @DataBoundSetter
    public void setVisibleItemCount(int visibleItemCount) {
        this.visibleItemCount = visibleItemCount;
    }

    @DataBoundSetter
    public void setMaxVersionCount(int maxVersionCount) {
        this.maxVersionCount = maxVersionCount;
    }

    public List<String> getGroupIdArtifactIdList() {
        String strippedChoices = StringUtils.trim(groupIdArtifactIds);
        if (StringUtils.isBlank(strippedChoices)) {
            return Collections.emptyList();
        }
        String[] choices = strippedChoices.split(CHOICES_DELIMITER);
        return Arrays.stream(choices)
                .map(StringUtils::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        log.log(Level.INFO, "Create value with jo. {0}", jo);
        JSONObject versionMap = jo.getJSONObject("value");
        List<String> value = new ArrayList<>(versionMap.size());
        for (Object obj : versionMap.values()) {
            value.add(obj.toString());
        }
        return new NexusArtifactChoicesParameterValue(getName(), value);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        log.log(Level.INFO, "Create value without jo.");
        try {
            JSONObject jo = req.getSubmittedForm();
            return createValue(req, jo);
        } catch (Exception e) {
            throw new RuntimeException("Create value error.", e);
        }
    }

    @Extension
    @Symbol("nexusArtifactChoices")
    public static class DescriptorImpl extends ParameterDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Nexus Artifact Choices Parameter";
        }

        public ListBoxModel doFillServerIdItems() {
            ListBoxModel items = new ListBoxModel();
            NexusRepoServerGlobalConfig.getInstance()
                    .getConfigs()
                    .forEach(e -> items.add(e.getDisplayName(), e.getServerId()));
            return items;
        }

        public ListBoxModel doFillVersionOptionsItems(
                @QueryParameter("serverId") String serverId,
                @QueryParameter("repository") String repository,
                @QueryParameter("option") String option,
                @QueryParameter("limits") int limits)
                throws Exception {
            ListBoxModel items = new ListBoxModel();
            NexusRepoServerConfig nxRepoCfg =
                    NexusRepoServerGlobalConfig.getConfig(serverId).orElseThrow();
            NexusRepositoryClient client = new NexusRepositoryClient(nxRepoCfg);
            String[] ss = option.split(":");
            final String groupId = ss[0];
            final String artifactId = ss[1];
            NexusSearchComponentsReq.NexusSearchComponentsReqBuilder reqBuilder =
                    NexusSearchComponentsReq.builder().groupId(groupId).artifactId(artifactId);
            if (client.isDocker()) {
                SearchDockerTagsResp resp = client.searchDockerTags(reqBuilder.build());
                String baseUrl = StringUtils.removeStart(client.getUrl(), "https://");
                baseUrl = StringUtils.removeStart(baseUrl, "http://");
                for (int i = resp.getTags().size() - 1; i >= 0; i--) {
                    String tag = resp.getTags().get(i);
                    String image = String.format("%s/%s:%s", baseUrl, resp.getName(), tag);
                    items.add(image, image);
                }
            } else {
                NexusRepositoryDetails nxRepo = client.getRepositoryDetails(repository);
                int loopNum = 0;
                String continuationToken = null;
                Set<String> versionSet = new LinkedHashSet<>();
                while (loopNum < 50 && versionSet.size() < limits) {
                    reqBuilder.continuationToken(continuationToken);
                    NexusSearchComponentsResp resp = client.searchComponents(nxRepo, reqBuilder.build());
                    if (CollectionUtils.isEmpty(resp.getItems())) {
                        break;
                    }
                    for (NexusComponentDetails c : resp.getItems()) {
                        String version = c.version(groupId, artifactId);
                        if (Objects.nonNull(version)) {
                            versionSet.add(option + ":" + version);
                        }
                    }
                    if (StringUtils.isBlank(resp.getContinuationToken())) {
                        break;
                    }
                    continuationToken = resp.getContinuationToken();
                    loopNum++;
                }
                versionSet.forEach(e -> items.add(e, e));
            }
            return items;
        }

        public FormValidation doCheckGroupIdArtifactIds(@QueryParameter String value) {
            if (NexusArtifactChoicesParameterDefinition.areValidGroupIdArtifactIds(value)) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.nexusArtifactParameterDefinition_invalidGroupIdArtifactIdPair());
            }
        }

        public FormValidation doCheckVisibleItemCount(@QueryParameter String value) {
            if (Utils.isNotEmpty(value)) {
                try {
                    int num = Integer.parseInt(value);
                    if (num <= 0) {
                        return FormValidation.error("Visible item count must greater than 0");
                    }
                } catch (NumberFormatException e) {
                    return FormValidation.error("Visible item count must be a number");
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckMaxVersionCount(@QueryParameter String value) {
            if (Utils.isNotEmpty(value)) {
                try {
                    int num = Integer.parseInt(value);
                    if (num <= 0) {
                        return FormValidation.error("Max version count must greater than 0");
                    }
                } catch (NumberFormatException e) {
                    return FormValidation.error("Max version count must be a number");
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doTest(
                @QueryParameter("repository") String repository, @QueryParameter("serverId") String serverId) {
            if (Utils.isNullOrEmpty(serverId)) {
                return FormValidation.error("Nexus Server Id is required");
            }
            NexusRepoServerConfig nxRepoCfg =
                    NexusRepoServerGlobalConfig.getConfig(serverId).orElse(null);
            if (Objects.isNull(nxRepoCfg)) {
                return FormValidation.error("Nexus Server ID not found");
            }
            if (!nxRepoCfg.isDocker() && Utils.isNullOrEmpty(repository)) {
                return FormValidation.error("Repository is required when nexus server is not docker !!!");
            }
            if (!nxRepoCfg.isDocker()) {
                try {
                    NexusRepositoryClient client = new NexusRepositoryClient(nxRepoCfg);
                    NexusRepositoryDetails nxRepo = client.getRepositoryDetails(repository);
                    if (NexusRepositoryFormat.docker.matches(nxRepo.getFormat())) {
                        return FormValidation.error("Repository is docker when nexus server is not docker !!!");
                    }
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Search nexus repository error", e);
                    return FormValidation.error(e, "Search nexus repository error");
                }
            }
            return FormValidation.ok();
        }
    }

    public static boolean areValidGroupIdArtifactIds(String value) {
        String strippedChoices = StringUtils.trim(value);
        if (StringUtils.isBlank(strippedChoices)) {
            return false;
        }
        String[] choices = strippedChoices.split(CHOICES_DELIMITER);
        if (ArrayUtils.isEmpty(choices)) {
            return false;
        }
        for (String choice : choices) {
            String[] pair = choice.split(":");
            if (pair.length != 2) {
                return false;
            }
        }
        return true;
    }
}
