package io.github.sonofmagic.javachanges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class JavaChangesMavenPluginSupport {

    private JavaChangesMavenPluginSupport() {
    }

    static String[] resolveCliArgs(String directory, String command, String[] arguments, String rawArgs) {
        List<String> effectiveArgs = new ArrayList<String>();
        if (ReleaseUtils.trimToNull(rawArgs) != null) {
            effectiveArgs.addAll(tokenize(rawArgs));
        } else {
            effectiveArgs.add(ReleaseUtils.trimToNull(command) == null ? "status" : command.trim());
            if (arguments != null) {
                for (String argument : arguments) {
                    String value = ReleaseUtils.trimToNull(argument);
                    if (value != null) {
                        effectiveArgs.add(value);
                    }
                }
            }
        }
        String directoryValue = ReleaseUtils.trimToNull(directory);
        if (directoryValue != null && !containsDirectoryOption(effectiveArgs)) {
            effectiveArgs.add(0, directoryValue);
            effectiveArgs.add(0, "--directory");
        }
        return effectiveArgs.toArray(new String[0]);
    }

    static List<String> tokenize(String rawArgs) {
        String value = ReleaseUtils.trimToNull(rawArgs);
        if (value == null) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        Character quote = null;
        boolean escaping = false;
        boolean tokenStarted = false;
        for (int index = 0; index < value.length(); index++) {
            char currentChar = value.charAt(index);
            if (escaping) {
                current.append(currentChar);
                escaping = false;
                tokenStarted = true;
                continue;
            }
            if (currentChar == '\\') {
                escaping = true;
                tokenStarted = true;
                continue;
            }
            if (quote != null) {
                if (currentChar == quote.charValue()) {
                    quote = null;
                } else {
                    current.append(currentChar);
                }
                tokenStarted = true;
                continue;
            }
            if (currentChar == '\'' || currentChar == '"') {
                quote = Character.valueOf(currentChar);
                tokenStarted = true;
                continue;
            }
            if (Character.isWhitespace(currentChar)) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }
            current.append(currentChar);
            tokenStarted = true;
        }
        if (quote != null) {
            throw new IllegalArgumentException("Unterminated quoted argument in javachanges.args");
        }
        if (escaping) {
            current.append('\\');
        }
        if (tokenStarted) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static boolean containsDirectoryOption(List<String> args) {
        for (String arg : args) {
            if ("--directory".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
