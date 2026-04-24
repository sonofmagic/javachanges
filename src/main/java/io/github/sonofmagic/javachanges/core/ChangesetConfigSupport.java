package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.CHANGESETS_DIR;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

public final class ChangesetConfigSupport {
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
        String snapshotVersionMode = field(json, "snapshotVersionMode");
        String tagStrategy = field(json, "tagStrategy");
        return ChangesetConfig.fromValues(baseBranch, releaseBranch, snapshotBranch, snapshotVersionMode, tagStrategy,
            trimToNull(baseBranch) != null,
            trimToNull(releaseBranch) != null,
            trimToNull(snapshotBranch) != null,
            trimToNull(snapshotVersionMode) != null,
            trimToNull(tagStrategy) != null);
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
        com.fasterxml.jackson.databind.JsonNode root = ReleaseJsonUtils.readTree(json);
        com.fasterxml.jackson.databind.JsonNode value = root.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
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

    public static final class ChangesetConfig {
        private final String baseBranch;
        private final String releaseBranch;
        private final String snapshotBranch;
        private final SnapshotVersionMode snapshotVersionMode;
        private final ReleaseTagStrategy tagStrategy;
        private final boolean explicitBaseBranch;
        private final boolean explicitReleaseBranch;
        private final boolean explicitSnapshotBranch;
        private final boolean explicitSnapshotVersionMode;
        private final boolean explicitTagStrategy;

        private ChangesetConfig(String baseBranch, String releaseBranch, String snapshotBranch,
                                SnapshotVersionMode snapshotVersionMode, ReleaseTagStrategy tagStrategy,
                                boolean explicitBaseBranch, boolean explicitReleaseBranch,
                                boolean explicitSnapshotBranch, boolean explicitSnapshotVersionMode,
                                boolean explicitTagStrategy) {
            this.baseBranch = baseBranch;
            this.releaseBranch = releaseBranch;
            this.snapshotBranch = snapshotBranch;
            this.snapshotVersionMode = snapshotVersionMode;
            this.tagStrategy = tagStrategy;
            this.explicitBaseBranch = explicitBaseBranch;
            this.explicitReleaseBranch = explicitReleaseBranch;
            this.explicitSnapshotBranch = explicitSnapshotBranch;
            this.explicitSnapshotVersionMode = explicitSnapshotVersionMode;
            this.explicitTagStrategy = explicitTagStrategy;
        }

        public static ChangesetConfig defaults() {
            return new ChangesetConfig("main", "changeset-release/main", "snapshot", SnapshotVersionMode.STAMPED,
                ReleaseTagStrategy.WHOLE_REPO, false, false, false, false, false);
        }

        static ChangesetConfig fromValues(String baseBranch, String releaseBranch, String snapshotBranch) {
            return fromValues(baseBranch, releaseBranch, snapshotBranch, null, null,
                trimToNull(baseBranch) != null,
                trimToNull(releaseBranch) != null,
                trimToNull(snapshotBranch) != null,
                false,
                false);
        }

        static ChangesetConfig fromValues(String baseBranch, String releaseBranch, String snapshotBranch,
                                          String snapshotVersionMode, String tagStrategy,
                                          boolean explicitBaseBranch, boolean explicitReleaseBranch,
                                          boolean explicitSnapshotBranch, boolean explicitSnapshotVersionMode,
                                          boolean explicitTagStrategy) {
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

            SnapshotVersionMode resolvedSnapshotVersionMode =
                SnapshotVersionMode.parse(snapshotVersionMode, SnapshotVersionMode.STAMPED);
            ReleaseTagStrategy resolvedTagStrategy =
                ReleaseTagStrategy.parse(tagStrategy, ReleaseTagStrategy.WHOLE_REPO);

            return new ChangesetConfig(resolvedBaseBranch, resolvedReleaseBranch, resolvedSnapshotBranch,
                resolvedSnapshotVersionMode, resolvedTagStrategy, explicitBaseBranch, explicitReleaseBranch,
                explicitSnapshotBranch, explicitSnapshotVersionMode, explicitTagStrategy);
        }

        public String baseBranch() {
            return baseBranch;
        }

        public String releaseBranch() {
            return releaseBranch;
        }

        public String snapshotBranch() {
            return snapshotBranch;
        }

        public SnapshotVersionMode snapshotVersionMode() {
            return snapshotVersionMode;
        }

        ReleaseTagStrategy tagStrategy() {
            return tagStrategy;
        }

        boolean hasBaseBranch() {
            return explicitBaseBranch;
        }

        boolean hasReleaseBranch() {
            return explicitReleaseBranch;
        }

        public boolean hasSnapshotBranch() {
            return explicitSnapshotBranch;
        }

        boolean hasSnapshotVersionMode() {
            return explicitSnapshotVersionMode;
        }

        boolean hasTagStrategy() {
            return explicitTagStrategy;
        }
    }
}
