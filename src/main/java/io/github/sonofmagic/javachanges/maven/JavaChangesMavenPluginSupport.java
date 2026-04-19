package io.github.sonofmagic.javachanges.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class JavaChangesMavenPluginSupport {

    private JavaChangesMavenPluginSupport() {
    }

    static String[] resolveCliArgs(String directory, String command, String[] arguments, String rawArgs) {
        if (trimToNull(rawArgs) != null) {
            return resolveRawCliArgs(directory, rawArgs);
        }
        return resolveStructuredCliArgs(directory, command, arguments);
    }

    static String[] resolveRawCliArgs(String directory, String rawArgs) {
        return prependDirectoryIfMissing(directory, tokenize(rawArgs));
    }

    static String[] resolveStructuredCliArgs(String directory, String command, String... arguments) {
        List<String> effectiveArgs = new ArrayList<String>();
        effectiveArgs.add(trimToNull(command) == null ? "status" : command.trim());
        if (arguments != null) {
            for (String argument : arguments) {
                String value = trimToNull(argument);
                if (value != null) {
                    effectiveArgs.add(value);
                }
            }
        }
        return prependDirectoryIfMissing(directory, effectiveArgs);
    }

    static List<String> tokenize(String rawArgs) {
        String value = trimToNull(rawArgs);
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

    static void addOption(List<String> args, String optionName, String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return;
        }
        args.add(optionName);
        args.add(trimmed);
    }

    static void addFlag(List<String> args, String optionName, boolean enabled) {
        if (enabled) {
            args.add(optionName);
            args.add("true");
        }
    }

    private static String[] prependDirectoryIfMissing(String directory, List<String> effectiveArgs) {
        String directoryValue = trimToNull(directory);
        if (directoryValue != null && !containsDirectoryOption(effectiveArgs)) {
            effectiveArgs.add(0, directoryValue);
            effectiveArgs.add(0, "--directory");
        }
        return effectiveArgs.toArray(new String[0]);
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
