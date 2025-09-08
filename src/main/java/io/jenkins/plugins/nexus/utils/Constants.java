package io.jenkins.plugins.nexus.utils;

public interface Constants {

    char C_SLASH = '/';
    String SLASH = "/";
    String GROUP = "group";
    String VERSION = "version";
    String SORT = "sort";
    String NAME = "name";
    String DIRECTION = "direction";
    String REPOSITORY = "repository";

    String IMAGE_TAG_SIG_REGEX = "^sha.*\\.sig$";

    String RAW_FILE_SIG_REGEX = ".*\\.sig$";

    String ECR = "ECR";
    String NEXUS = "NEXUS";

}
