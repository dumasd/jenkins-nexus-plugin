package io.jenkins.plugins.nexus.utils;

import java.io.File;
import java.util.regex.Pattern;

public class Utils {

    private Utils() {}

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

    public static String toNexusDictionary(String groupId, String artifactId) {
        String groupPath = groupId.replace('.', '/');
        return "/" + Utils.splicePath(groupPath, artifactId) + "/";
    }

    public static String splicePath(String... ps) {
        StringBuilder builder = new StringBuilder();
        for (String p : ps) {
            if (isNotEmpty(p)) {
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

    public static boolean isFile(String pathString) {
        File file = new File(pathString);
        // 检查路径是否存在
        if (file.exists()) {
            return file.isFile();
        } else {
            return !(pathString.endsWith("/") || pathString.endsWith("\\\\"));
        }
    }

    public static String getFileName(String path) {
        if (isNullOrEmpty(path)) {
            return "";
        }
        int idx = path.lastIndexOf(Constants.C_SLASH);
        if (idx == path.length() - 1) {
            return "";
        }
        return idx < 0 ? path : path.substring(idx + 1);
    }

    public static boolean isMatch(Pattern pattern, String content) {
        if (content == null) {
            return false;
        }
        return pattern.matcher(content).matches();
    }

    public static boolean isNotContains(String text, String search) {
        if (text == null || search == null) {
            return true;
        }
        return !text.contains(search);
    }
}
