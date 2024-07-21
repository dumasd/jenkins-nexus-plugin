package io.jenkins.plugins.nexus.utils;

import java.util.Objects;

/**
 * @author Bruce.Wu
 * @date 2024-07-21
 */
public enum NexusRepositoryType {
    hosted,
    proxy,
    group,
    ;

    public static boolean isSupported(String name) {
        return Objects.equals(name, hosted.name());
    }
}
