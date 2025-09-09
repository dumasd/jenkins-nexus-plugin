package io.jenkins.plugins.nexus;

import static hudson.model.ChoiceParameterDefinition.CHOICES_DELIMITER;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.choice.ArtifactChoiceHandler;
import io.jenkins.plugins.nexus.choice.ArtifactChoiceHandlers;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.config.NexusRepoServerGlobalConfig;
import io.jenkins.plugins.nexus.model.resp.NexusRepositoryDetails;
import io.jenkins.plugins.nexus.utils.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @date 2024-07-22
 */
@Setter
@Getter
@Log
public class NexusArtifactChoicesParameterDefinition extends ParameterDefinition {

    private static final long serialVersionUID = -6584524453125089068L;

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
    private int maxVersionCount = 100;

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

    public ListBoxModel getGroupIdArtifactIdList() {
        String strippedChoices = StringUtils.trim(groupIdArtifactIds);
        ListBoxModel result = new ListBoxModel();
        if (StringUtils.isBlank(strippedChoices)) {
            return result;
        }
        String[] choices = strippedChoices.split(CHOICES_DELIMITER);
        Arrays.stream(choices)
                .map(Util::fixEmptyAndTrim)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(e -> {
                    String[] ss = e.split(":");
                    result.add(ss[0] + ":" + ss[1], e);
                });
        return result;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        String value = jo.getString("value");
        String name = jo.getString("name");
        return new NexusArtifactChoicesParameterValue(name, value);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {

        String parameterName = getName();

        String value = req.getParameter(parameterName);
        if (value != null) {
            return new NexusArtifactChoicesParameterValue(parameterName, value);
        }

        String[] parameterValues = req.getParameterValues(parameterName);
        if (parameterValues != null && parameterValues.length > 0) {
            value = String.join(",", parameterValues);
            return new NexusArtifactChoicesParameterValue(parameterName, value);
        }

        try {
            JSONObject jo = req.getSubmittedForm();

            Object obj = jo.getOrDefault(parameterName, null);
            if (obj instanceof CharSequence) {
                return new NexusArtifactChoicesParameterValue(parameterName, obj.toString());
            } else if (obj instanceof JSONArray) {
                JSONArray values = (JSONArray) obj;
                value = values.stream().map(Object::toString).collect(Collectors.joining(","));
                if (StringUtils.isNotBlank(value)) {
                    return new NexusArtifactChoicesParameterValue(parameterName, value);
                }
            }

            JSONArray parameters = jo.getJSONArray("parameter");
            for (int i = 0; i < parameters.size(); i++) {
                JSONObject parameter = parameters.getJSONObject(i);
                String name = parameter.getString("name");
                if (parameterName.equals(name)) {
                    return createValue(req, parameter);
                }
            }
            return new NexusArtifactChoicesParameterValue(parameterName, "");
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

        @POST
        @JavaScriptMethod(name = "fillVersionOptionsItems")
        public ListBoxModel doFillVersionOptionsItems(
                @QueryParameter("serverId") String serverId,
                @QueryParameter("repository") String repository,
                @QueryParameter("option") String option,
                @QueryParameter("limits") int limits,
                @QueryParameter("keyword") String keyword)
                throws Exception {
            ListBoxModel items;
            NexusRepoServerConfig nxRepoCfg =
                    NexusRepoServerGlobalConfig.getConfig(serverId).orElseThrow();

            if (nxRepoCfg.isDocker()) {
                Registry registry = Utils.isNullOrEmpty(nxRepoCfg.getRegistry())
                        ? Registry.NEXUS
                        : Registry.valueOf(nxRepoCfg.getRegistry());
                ArtifactChoiceHandler artifactChoiceHandler = ArtifactChoiceHandlers.getDockerHandler(registry);
                items = artifactChoiceHandler.getItems(nxRepoCfg, option, repository, limits);
            } else {
                ArtifactChoiceHandler artifactChoiceHandler = ArtifactChoiceHandlers.getHandler();
                items = artifactChoiceHandler.getItems(nxRepoCfg, option, repository, limits);
            }

            if (Utils.isNotEmpty(keyword)) {
                // 筛选
                items.removeIf(e -> Utils.isNotContains(e.value, keyword) || Utils.isNotContains(e.name, keyword));
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
            if (pair.length < 2 || pair.length > 3) {
                return false;
            }
        }
        return true;
    }
}
