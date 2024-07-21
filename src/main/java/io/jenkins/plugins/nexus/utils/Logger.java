package io.jenkins.plugins.nexus.utils;

import hudson.model.TaskListener;
import java.io.PrintStream;

/**
 * Provides logging utilities for error and debug message handling within the application.
 * This class is designed to standardize the format of log messages and centralize the handling
 * of logging operations, particularly for interactions with the Lark platform. It supports
 * formatting messages, logging errors, and conditional debug logging based on global configuration settings.
 *
 * @author bruce.wu
 */
public class Logger {

    private final String prefix;
    private final TaskListener listener;

    public Logger(String prefix, TaskListener listener) {
        this.prefix = prefix;
        this.listener = listener;
    }

    /**
     * Logs an error message through the specified task listener. The message is formatted and
     * prefixed to indicate it is an error related to Lark operations.
     *
     * @param msg  The error message template.
     * @param args Arguments for the message template.
     */
    public void error(String msg, Object... args) {
        listener.error("[" + prefix + "] error: %s", String.format(msg, args));
    }

    /**
     * Logs a debug message through the specified task listener. The message is printed directly
     * to the listener's logger, allowing for real-time debugging information to be output.
     *
     * @param msg  The debug message template.
     * @param args Arguments for the message template.
     */
    private void debug(String msg, Object... args) {
        PrintStream logger = listener.getLogger();
        logger.printf(msg + "%n", args);
    }

    /**
     * Logs a message through the specified task listener, depending on the global verbose configuration.
     * If verbose logging is enabled, the message is logged as a debug message; otherwise, it is ignored.
     * This allows for conditional logging based on the application's current configuration settings.
     *
     * @param msg  The message template.
     * @param args Arguments for the message template.
     */
    public void log(String msg, Object... args) {
        debug("[" + prefix + "] " + msg, args);
    }

    public PrintStream getStream() {
        return listener.getLogger();
    }
}
