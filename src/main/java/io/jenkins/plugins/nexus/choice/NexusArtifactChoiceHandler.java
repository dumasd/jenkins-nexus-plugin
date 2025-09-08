package io.jenkins.plugins.nexus.choice;

import hudson.Util;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.model.req.NexusSearchComponentsReq;
import io.jenkins.plugins.nexus.model.resp.NexusComponentDetails;
import io.jenkins.plugins.nexus.model.resp.NexusRepositoryDetails;
import io.jenkins.plugins.nexus.model.resp.NexusSearchComponentsResp;
import io.jenkins.plugins.nexus.model.resp.SearchDockerTagsResp;
import io.jenkins.plugins.nexus.utils.Constants;
import io.jenkins.plugins.nexus.utils.NexusRepositoryClient;
import io.jenkins.plugins.nexus.utils.Utils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.selectors.SelectorUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

public class NexusArtifactChoiceHandler implements ArtifactChoiceHandler {
    @Override
    public ListBoxModel getItems(NexusRepoServerConfig serverConfig, String option, String repository, int limits) {
        ListBoxModel items = new ListBoxModel();
        NexusRepositoryClient client = new NexusRepositoryClient(serverConfig);

        String[] groupArtifactFilter = option.split(":");
        final String groupId = groupArtifactFilter[0];
        final String artifactId = groupArtifactFilter[1];
        final String filter = groupArtifactFilter.length > 2 ? Util.fixEmptyAndTrim(groupArtifactFilter[2]) : null;
        Function<String, Boolean> filterFunc = s -> filter == null || SelectorUtils.match(filter, s);

        NexusSearchComponentsReq.NexusSearchComponentsReqBuilder reqBuilder =
                NexusSearchComponentsReq.builder().groupId(groupId).artifactId(artifactId);

        if (client.isDocker()) {
            Pattern cosignSignTagPattern = Pattern.compile(Constants.IMAGE_TAG_SIG_REGEX);
            SearchDockerTagsResp resp = client.searchDockerTags(reqBuilder.build());
            String baseUrl = StringUtils.removeStart(client.getUrl(), "https://");
            baseUrl = StringUtils.removeStart(baseUrl, "http://");
            for (int i = resp.getTags().size() - 1; i >= 0; i--) {
                String tag = resp.getTags().get(i);
                if ((!Utils.isMatch(cosignSignTagPattern, tag)) && filterFunc.apply(tag)) {
                    String image = String.format("%s/%s:%s", baseUrl, resp.getName(), tag);
                    items.add(image, image);
                }
            }
        } else {
            Pattern cosignSignTagPattern = Pattern.compile(Constants.RAW_FILE_SIG_REGEX);
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
                    if (Utils.isMatch(cosignSignTagPattern, c.getName())) {
                        continue;
                    }
                    String version = c.version(groupId, artifactId);
                    if (Objects.nonNull(version) && filterFunc.apply(version)) {
                        versionSet.add(String.format("%s:%s:%s", groupId, artifactId, version));
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

}
