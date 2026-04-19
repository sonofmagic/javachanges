package io.github.sonofmagic.javachanges.core;

import java.io.Console;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.normalizeType;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.parseModules;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class ChangesetInput {
    final ReleaseLevel release;
    final String type;
    final List<String> modules;
    final String summary;
    final String body;

    ChangesetInput(ReleaseLevel release, String type, List<String> modules, String summary, String body) {
        this.release = release;
        this.type = type;
        this.modules = modules;
        this.summary = summary;
        this.body = body;
    }
}

final class Changeset {
    final Path path;
    final String fileName;
    final ReleaseLevel release;
    final String type;
    final List<String> modules;
    final String summary;
    final String body;

    Changeset(Path path, String fileName, ReleaseLevel release, String type,
              List<String> modules, String summary, String body) {
        this.path = path;
        this.fileName = fileName;
        this.release = release;
        this.type = type;
        this.modules = modules;
        this.summary = summary;
        this.body = body;
    }
}

final class ChangesetPrompter {
    private ChangesetPrompter() {
    }

    static ChangesetInput resolveInput(Path repoRoot, Map<String, String> options, PrintStream out, PrintStream err) {
        String summary = trimToNull(options.get("summary"));
        String release = trimToNull(options.get("release"));
        String type = trimToNull(options.get("type"));
        String modules = trimToNull(options.get("modules"));
        String body = trimToNull(options.get("body"));

        if (summary == null) {
            summary = trimToNull(System.getenv("CHANGESET_SUMMARY"));
        }
        if (release == null) {
            release = trimToNull(System.getenv("CHANGESET_RELEASE"));
        }
        if (type == null) {
            type = trimToNull(System.getenv("CHANGESET_TYPE"));
        }
        if (modules == null) {
            modules = trimToNull(System.getenv("CHANGESET_MODULES"));
        }
        if (body == null) {
            body = trimToNull(System.getenv("CHANGESET_BODY"));
        }

        if (summary != null && release != null) {
            return new ChangesetInput(
                ReleaseLevel.parse(release),
                normalizeType(type == null ? "other" : type),
                parseModules(repoRoot, modules == null ? "all" : modules),
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
            normalizeType(type),
            parseModules(repoRoot, modules),
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
            value = trimToNull(value);
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

final class Slug {
    private Slug() {
    }

    static String slugify(String text) {
        StringBuilder builder = new StringBuilder();
        char previous = '-';
        for (int i = 0; i < text.length(); i++) {
            char current = Character.toLowerCase(text.charAt(i));
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9')) {
                builder.append(current);
                previous = current;
            } else if (previous != '-') {
                builder.append('-');
                previous = '-';
            }
        }
        String slug = builder.toString();
        while (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        if (slug.isEmpty()) {
            return "changeset";
        }
        return slug.length() > 48 ? slug.substring(0, 48) : slug;
    }
}
