package io.jenkins.plugins.nexus.utils;

/**
 * @author Bruce.Wu
 * @date 2024-07-21
 */
public enum NexusRepositoryFormat {
    maven2,
    raw,
    docker,
    apt,
    npm,
    nuget,
    r,
    yum,
    bower,
    ;

    public boolean matches(String name) {
        return this.name().equals(name);
    }

    public static boolean isSupported(String format) {
        return maven2.name().equals(format) || raw.name().equals(format);
    }
}
