package io.jenkins.plugins.nexus.utils;

import java.io.File;

public class Utils {

    public static boolean isNullOrEmpty(final String name) {
        return name == null || name.matches("\\s*");
    }

    public static boolean isNotEmpty(final String name) {
        return !isNullOrEmpty(name);
    }

    public static String getFileExt(File file) {
        String name = file.getName();
        int idx = name.indexOf('.');
        if (idx > 0) {
            return name.substring(idx + 1);
        } else {
            return null;
        }
    }

    public static String splicePath(String... ps) {
        StringBuilder builder = new StringBuilder();
        for (String p : ps) {
            if (!isNullOrEmpty(p)) {
                if (builder.length() != 0) {
                    if (builder.toString().endsWith(Constants.SLASH) && p.startsWith(Constants.SLASH)) {
                        builder.append(p.substring(1));
                    } else if (builder.toString().endsWith(Constants.SLASH)) {
                        builder.append(p);
                    } else {
                        builder.append(Constants.SLASH).append(p);
                    }
                } else {
                    builder.append(p);
                }
            }
        }
        return builder.toString();
    }
}
