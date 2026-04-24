package io.github.sonofmagic.javachanges.core.changeset;

import io.github.sonofmagic.javachanges.core.ReleaseLevel;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.Console;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

public final class ChangesetPrompter {
    private ChangesetPrompter() {
    }

    public static ChangesetInput resolveInput(Path repoRoot, Map<String, String> options, PrintStream out, PrintStream err) {
        String summary = ReleaseTextUtils.trimToNull(options.get("summary"));
        String release = ReleaseTextUtils.trimToNull(options.get("release"));
        String type = ReleaseTextUtils.trimToNull(options.get("type"));
        String modules = ReleaseTextUtils.trimToNull(options.get("modules"));
        String body = ReleaseTextUtils.trimToNull(options.get("body"));

        if (summary == null) {
            summary = ReleaseTextUtils.trimToNull(System.getenv("CHANGESET_SUMMARY"));
        }
        if (release == null) {
            release = ReleaseTextUtils.trimToNull(System.getenv("CHANGESET_RELEASE"));
        }
        if (type == null) {
            type = ReleaseTextUtils.trimToNull(System.getenv("CHANGESET_TYPE"));
        }
        if (modules == null) {
            modules = ReleaseTextUtils.trimToNull(System.getenv("CHANGESET_MODULES"));
        }
        if (body == null) {
            body = ReleaseTextUtils.trimToNull(System.getenv("CHANGESET_BODY"));
        }

        if (summary != null && release != null) {
            return new ChangesetInput(
                ReleaseLevel.parse(release),
                ReleaseModuleUtils.normalizeType(type == null ? "other" : type),
                ReleaseModuleUtils.parseModules(repoRoot, modules == null ? "all" : modules),
                summary,
                body == null ? "" : body
            );
        }

        Console console = System.console();
        Scanner scanner = console == null ? new Scanner(System.in, "UTF-8") : null;

        summary = summary != null ? summary : prompt(console, scanner, out, "Summary");
        release = release != null ? release : prompt(console, scanner, out, "Release level (patch/minor/major)");
        type = type == null ? "other" : type;
        modules = modules == null ? "all" : modules;

        if (body == null) {
            out.println("Body (optional, finish with a single `.` line):");
            body = readMultiline(console, scanner);
        }

        return new ChangesetInput(
            ReleaseLevel.parse(release),
            ReleaseModuleUtils.normalizeType(type),
            ReleaseModuleUtils.parseModules(repoRoot, modules),
            summary,
            body
        );
    }

    private static String prompt(Console console, Scanner scanner, PrintStream out, String label) {
        String value;
        do {
            if (console != null) {
                value = console.readLine("%s: ", label);
            } else {
                out.print(label + ": ");
                out.flush();
                value = scanner.nextLine();
            }
            value = ReleaseTextUtils.trimToNull(value);
        } while (value == null);
        return value;
    }

    private static String readMultiline(Console console, Scanner scanner) {
        StringBuilder builder = new StringBuilder();
        while (true) {
            String line = console != null ? console.readLine() : scanner.nextLine();
            if (".".equals(line)) {
                break;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString().trim();
    }
}
