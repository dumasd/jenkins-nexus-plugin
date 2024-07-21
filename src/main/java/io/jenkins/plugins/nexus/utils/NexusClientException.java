package io.jenkins.plugins.nexus.utils;

public class NexusClientException extends RuntimeException {
    public NexusClientException(String message) {
        super(message);
    }

    public NexusClientException(Throwable cause) {
        super(cause);
    }
}
