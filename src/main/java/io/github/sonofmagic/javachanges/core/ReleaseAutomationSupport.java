package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlanner;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;

public final class ReleaseAutomationSupport {
    private static final String CHANGESETS_DIR = ".changesets";
    private static final String RELEASE_PLAN_JSON = "release-plan.json";
    private static final String RELEASE_PLAN_MD = "release-plan.md";

    private final Path repoRoot;

    public ReleaseAutomationSupport(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public ReleasePlan plan() throws IOException, InterruptedException {
        return new ReleasePlanner(repoRoot).plan();
    }

    public ReleaseDescriptor descriptorFromPlan(ReleasePlan plan) {
        return new ReleaseDescriptor(plan.getReleaseVersion(), plan.getTagStrategy(), plan.getReleaseTargets());
    }

    public ReleaseDescriptor descriptorFromManifest() throws IOException {
        JsonNode manifest = readManifest();
        String releaseVersion = requiredText(manifest, "releaseVersion");
        ReleaseTagStrategy tagStrategy = ReleaseTagStrategy.parse(textOrNull(manifest.get("tagStrategy")),
            ReleaseTagStrategy.WHOLE_REPO);
        List<ReleasePlan.ReleaseTarget> releaseTargets = readReleaseTargets(manifest, releaseVersion, tagStrategy);
        return new ReleaseDescriptor(releaseVersion, tagStrategy, releaseTargets);
    }

    public String releaseVersionFromManifest() throws IOException {
        return RepoFiles.readManifestField(repoRoot, "releaseVersion");
    }

    public String wholeRepoTagFromManifest() throws IOException {
        return "v" + releaseVersionFromManifest();
    }

    public Path releasePlanMarkdownFile() {
        return repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_MD);
    }

    public static final class ReleaseDescriptor {
        public final String releaseVersion;
        public final ReleaseTagStrategy tagStrategy;
        public final List<ReleasePlan.ReleaseTarget> releaseTargets;

        ReleaseDescriptor(String releaseVersion, ReleaseTagStrategy tagStrategy, List<ReleasePlan.ReleaseTarget> releaseTargets) {
            this.releaseVersion = releaseVersion;
            this.tagStrategy = tagStrategy;
            this.releaseTargets = Collections.unmodifiableList(new ArrayList<ReleasePlan.ReleaseTarget>(releaseTargets));
        }

        public String commitMessage() {
            return "chore(release): apply changesets for v" + releaseVersion;
        }

        public String githubPullRequestTitle() {
            return "chore(release): v" + releaseVersion;
        }

        public String gitlabMergeRequestTitle() {
            return "chore(release): release v" + releaseVersion;
        }

        public String wholeRepoTagName() {
            return "v" + releaseVersion;
        }

        public List<String> tagNames() {
            List<String> tags = new ArrayList<String>();
            for (ReleasePlan.ReleaseTarget target : releaseTargets) {
                tags.add(target.tag);
            }
            return tags;
        }

        public String primaryTagName() {
            if (releaseTargets.isEmpty()) {
                return wholeRepoTagName();
            }
            if (releaseTargets.size() == 1) {
                return releaseTargets.get(0).tag;
            }
            for (ReleasePlan.ReleaseTarget target : releaseTargets) {
                if (target.module == null) {
                    return target.tag;
                }
            }
            throw new IllegalStateException("Release plan defines multiple tags under per-module strategy. "
                + "Use explicit tag-based release commands instead of release-from-plan.");
        }
    }

    private JsonNode readManifest() throws IOException {
        return ReleaseJsonUtils.readTree(new String(
            java.nio.file.Files.readAllBytes(repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_JSON)),
            StandardCharsets.UTF_8
        ));
    }

    private static String requiredText(JsonNode node, String field) {
        String value = textOrNull(node.get(field));
        if (value == null) {
            throw new IllegalStateException("Missing field " + field + " in release manifest");
        }
        return value;
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static List<ReleasePlan.ReleaseTarget> readReleaseTargets(JsonNode manifest, String releaseVersion,
                                                                      ReleaseTagStrategy tagStrategy) {
        List<ReleasePlan.ReleaseTarget> targets = new ArrayList<ReleasePlan.ReleaseTarget>();
        JsonNode releaseTargetsNode = manifest.get("releaseTargets");
        if (releaseTargetsNode != null && releaseTargetsNode.isArray()) {
            for (JsonNode node : releaseTargetsNode) {
                String module = textOrNull(node.get("module"));
                String tag = textOrNull(node.get("tag"));
                if (tag != null) {
                    targets.add(new ReleasePlan.ReleaseTarget(module, tag));
                }
            }
            if (!targets.isEmpty()) {
                return targets;
            }
        }
        if (tagStrategy == ReleaseTagStrategy.PER_MODULE) {
            JsonNode changesetsNode = manifest.get("changesets");
            java.util.LinkedHashSet<String> modules = new java.util.LinkedHashSet<String>();
            if (changesetsNode != null && changesetsNode.isArray()) {
                for (JsonNode changeset : changesetsNode) {
                    JsonNode modulesNode = changeset.get("modules");
                    if (modulesNode != null && modulesNode.isArray()) {
                        for (JsonNode moduleNode : modulesNode) {
                            String module = textOrNull(moduleNode);
                            if (module != null) {
                                modules.add(module);
                            }
                        }
                    }
                }
            }
            for (String module : modules) {
                targets.add(new ReleasePlan.ReleaseTarget(module, module + "/v" + releaseVersion));
            }
            if (!targets.isEmpty()) {
                return targets;
            }
        }
        targets.add(new ReleasePlan.ReleaseTarget(null, "v" + releaseVersion));
        return targets;
    }
}
