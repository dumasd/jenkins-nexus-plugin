package io.jenkins.plugins.nexus.choice;

import com.amazonaws.auth.*;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.*;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Util;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nexus.config.NexusRepoServerConfig;
import io.jenkins.plugins.nexus.utils.Constants;
import io.jenkins.plugins.nexus.utils.Utils;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.selectors.SelectorUtils;

public class ECRArtifactChoiceHandler implements ArtifactChoiceHandler {
    @Override
    public ListBoxModel getItems(NexusRepoServerConfig serverConfig, String option, String repository, int limits) {
        AmazonECR ecr = createECR(serverConfig);

        ListBoxModel items = new ListBoxModel();

        String[] groupArtifactFilter = option.split(":");
        final String groupId = groupArtifactFilter[0];
        final String artifactId = groupArtifactFilter[1];
        final String filter = groupArtifactFilter.length > 2 ? Util.fixEmptyAndTrim(groupArtifactFilter[2]) : null;
        Function<String, Boolean> filterFunc = s -> filter == null || SelectorUtils.match(filter, s);

        String repositoryName;
        if (StringUtils.isNotBlank(groupId)) {
            repositoryName = groupId + "/" + artifactId;
        } else {
            repositoryName = artifactId;
        }

        // 1. 获取仓库信息
        DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
        describeRepositoriesRequest.setRepositoryNames(Collections.singletonList(repositoryName));
        DescribeRepositoriesResult describeRepositoriesResult = ecr.describeRepositories(describeRepositoriesRequest);
        Repository ecrRepository = describeRepositoriesResult.getRepositories().get(0);
        String baseUri = ecrRepository.getRepositoryUri();

        // 2.列出tag列表
        Pattern cosignSignTagPattern = Pattern.compile(Constants.IMAGE_TAG_SIG_REGEX);
        String continuationToken = null;
        Set<String> versionSet = new LinkedHashSet<>();
        int loopNum = 0;
        while (loopNum < 10 && versionSet.size() < limits) {
            ListImagesRequest listImagesRequest = new ListImagesRequest();
            listImagesRequest.setRepositoryName(repositoryName);
            listImagesRequest.setMaxResults(500);
            listImagesRequest.setNextToken(continuationToken);
            ListImagesResult listImagesResult = ecr.listImages(listImagesRequest);
            for (ImageIdentifier imageIdentifier : listImagesResult.getImageIds()) {
                if ((!Utils.isMatch(cosignSignTagPattern, imageIdentifier.getImageTag()))
                        && filterFunc.apply(imageIdentifier.getImageTag())) {
                    versionSet.add(baseUri + ":" + imageIdentifier.getImageTag());
                }
            }
            continuationToken = listImagesRequest.getNextToken();

            if (continuationToken == null) {
                break;
            }
            loopNum++;
        }
        versionSet.forEach(e -> items.add(e, e));
        return items;
    }

    private AmazonECR createECR(NexusRepoServerConfig serverConfig) {
        List<StandardUsernamePasswordCredentials> usernamePasswordCredentialsList =
                CredentialsProvider.lookupCredentialsInItemGroup(
                        StandardUsernamePasswordCredentials.class, Jenkins.get(), null, Collections.emptyList());
        Optional<StandardUsernamePasswordCredentials> usernamePasswordCredentials =
                usernamePasswordCredentialsList.stream()
                        .filter(e -> Objects.equals(e.getId(), serverConfig.getCredentialsId()))
                        .findFirst();
        AWSCredentialsProvider awsCredentialsProvider = null;
        if (usernamePasswordCredentials.isPresent()) {
            StandardUsernamePasswordCredentials credentials = usernamePasswordCredentials.get();
            AWSCredentials awsCredentials = new BasicAWSCredentials(
                    credentials.getUsername(), credentials.getPassword().getPlainText());
            awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        } else {
            List<AmazonWebServicesCredentials> amazonWebServicesCredentialsList =
                    CredentialsProvider.lookupCredentialsInItemGroup(
                            AmazonWebServicesCredentials.class, Jenkins.get(), null, Collections.emptyList());
            Optional<AmazonWebServicesCredentials> amazonWebServicesCredentials =
                    amazonWebServicesCredentialsList.stream()
                            .filter(e -> Objects.equals(e.getId(), serverConfig.getCredentialsId()))
                            .findFirst();
            if (amazonWebServicesCredentials.isPresent()) {
                AmazonWebServicesCredentials credentials = amazonWebServicesCredentials.get();
                AWSCredentials awsCredentials = credentials.getCredentials();
                awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
            }
        }
        AmazonECRClientBuilder amazonECRClientBuilder = AmazonECRClientBuilder.standard();
        if (awsCredentialsProvider != null) {
            amazonECRClientBuilder.withCredentials(awsCredentialsProvider);
        }

        amazonECRClientBuilder.withEndpointConfiguration(new AmazonECRClientBuilder.EndpointConfiguration(
                serverConfig.getServerUrl(), serverConfig.getRegion()));
        return amazonECRClientBuilder.build();
    }
}
