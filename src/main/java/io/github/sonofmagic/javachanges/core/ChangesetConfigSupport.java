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

    private ChangesetConfigSupport() {
    }

    static ChangesetConfig load(Path repoRoot) throws IOException {
        Path configPath = repoRoot.resolve(CHANGESETS_DIR).resolve(CONFIG_JSON);
        if (!Files.exists(configPath)) {
            return ChangesetConfig.defaults();
        }
        String json = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
        String baseBranch = field(json, "baseBranch");
        String releaseBranch = field(json, "releaseBranch");
        String snapshotBranch = field(json, "snapshotBranch");
        return ChangesetConfig.fromValues(baseBranch, releaseBranch, snapshotBranch);
    }

    private static String field(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return ReleaseJsonUtils.jsonUnescape(matcher.group(1));
    }

    static final class ChangesetConfig {
        private final String baseBranch;
        private final String releaseBranch;
        private final String snapshotBranch;

        private ChangesetConfig(String baseBranch, String releaseBranch, String snapshotBranch) {
            this.baseBranch = baseBranch;
            this.releaseBranch = releaseBranch;
            this.snapshotBranch = snapshotBranch;
        }

        static ChangesetConfig defaults() {
            return new ChangesetConfig("main", "changeset-release/main", "snapshot");
        }

        static ChangesetConfig fromValues(String baseBranch, String releaseBranch, String snapshotBranch) {
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

            return new ChangesetConfig(resolvedBaseBranch, resolvedReleaseBranch, resolvedSnapshotBranch);
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
    }
}
