package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.CHANGESETS_DIR;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class ChangesetConfigSupport {
    private static final String CONFIG_JSON = "config.json";
    private static final String CONFIG_JSONC = "config.jsonc";

    private ChangesetConfigSupport() {
    }

    static ChangesetConfig load(Path repoRoot) throws IOException {
        Path configPath = resolveConfigPath(repoRoot);
        if (configPath == null) {
            return ChangesetConfig.defaults();
        }
        String json = stripJsonComments(new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8));
        String baseBranch = field(json, "baseBranch");
        String releaseBranch = field(json, "releaseBranch");
        String snapshotBranch = field(json, "snapshotBranch");
        return ChangesetConfig.fromValues(baseBranch, releaseBranch, snapshotBranch,
            trimToNull(baseBranch) != null,
            trimToNull(releaseBranch) != null,
            trimToNull(snapshotBranch) != null);
    }

    static Path resolveConfigRoot(Path start) {
        Path probe = start.toAbsolutePath().normalize();
        while (probe != null) {
            if (resolveConfigPath(probe) != null) {
                return probe;
            }
            probe = probe.getParent();
        }
        return null;
    }

    static Path resolveConfigPath(Path repoRoot) {
        Path changesetsDir = repoRoot.resolve(CHANGESETS_DIR);
        Path jsonPath = changesetsDir.resolve(CONFIG_JSON);
        if (Files.exists(jsonPath)) {
            return jsonPath;
        }
        Path jsoncPath = changesetsDir.resolve(CONFIG_JSONC);
        if (Files.exists(jsoncPath)) {
            return jsoncPath;
        }
        return null;
    }

    private static String field(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return ReleaseJsonUtils.jsonUnescape(matcher.group(1));
    }

    private static String stripJsonComments(String text) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);

            if (inString) {
                result.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                result.append(current);
                continue;
            }

            if (current == '/' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == '/') {
                    i += 2;
                    while (i < text.length() && text.charAt(i) != '\n' && text.charAt(i) != '\r') {
                        i++;
                    }
                    if (i < text.length()) {
                        result.append(text.charAt(i));
                    }
                    continue;
                }
                if (next == '*') {
                    i += 2;
                    while (i + 1 < text.length() && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                        i++;
                    }
                    i++;
                    continue;
                }
            }

            result.append(current);
        }

        return result.toString();
    }

    static final class ChangesetConfig {
        private final String baseBranch;
        private final String releaseBranch;
        private final String snapshotBranch;
        private final boolean explicitBaseBranch;
        private final boolean explicitReleaseBranch;
        private final boolean explicitSnapshotBranch;

        private ChangesetConfig(String baseBranch, String releaseBranch, String snapshotBranch,
                                boolean explicitBaseBranch, boolean explicitReleaseBranch,
                                boolean explicitSnapshotBranch) {
            this.baseBranch = baseBranch;
            this.releaseBranch = releaseBranch;
            this.snapshotBranch = snapshotBranch;
            this.explicitBaseBranch = explicitBaseBranch;
            this.explicitReleaseBranch = explicitReleaseBranch;
            this.explicitSnapshotBranch = explicitSnapshotBranch;
        }

        static ChangesetConfig defaults() {
            return new ChangesetConfig("main", "changeset-release/main", "snapshot", false, false, false);
        }

        static ChangesetConfig fromValues(String baseBranch, String releaseBranch, String snapshotBranch) {
            return fromValues(baseBranch, releaseBranch, snapshotBranch,
                trimToNull(baseBranch) != null,
                trimToNull(releaseBranch) != null,
                trimToNull(snapshotBranch) != null);
        }

        static ChangesetConfig fromValues(String baseBranch, String releaseBranch, String snapshotBranch,
                                          boolean explicitBaseBranch, boolean explicitReleaseBranch,
                                          boolean explicitSnapshotBranch) {
            String resolvedBaseBranch = trimToNull(baseBranch);
            if (resolvedBaseBranch == null) {
                resolvedBaseBranch = "main";
            }

            String resolvedReleaseBranch = trimToNull(releaseBranch);
            if (resolvedReleaseBranch == null) {
                resolvedReleaseBranch = "changeset-release/" + resolvedBaseBranch;
            }

            String resolvedSnapshotBranch = trimToNull(snapshotBranch);
            if (resolvedSnapshotBranch == null) {
                resolvedSnapshotBranch = "snapshot";
            }

            return new ChangesetConfig(resolvedBaseBranch, resolvedReleaseBranch, resolvedSnapshotBranch,
                explicitBaseBranch, explicitReleaseBranch, explicitSnapshotBranch);
        }

        String baseBranch() {
            return baseBranch;
        }

        String releaseBranch() {
            return releaseBranch;
        }

        String snapshotBranch() {
            return snapshotBranch;
        }

        boolean hasBaseBranch() {
            return explicitBaseBranch;
        }

        boolean hasReleaseBranch() {
            return explicitReleaseBranch;
        }

        boolean hasSnapshotBranch() {
            return explicitSnapshotBranch;
        }
    }
}
