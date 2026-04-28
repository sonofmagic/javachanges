package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.sonofmagic.javachanges.core.config.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlanner;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;

public final class ReleaseAutomationSupport {
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

    public ReleaseDescriptor descriptorFromFreshPlan() throws IOException, InterruptedException {
        ReleasePlan plan = plan();
        if (plan.hasPendingChangesets()) {
            return descriptorFromPlan(plan);
        }
        String releaseVersion = ReleaseTextUtils.stripSnapshot(BuildModelSupport.readRevision(repoRoot));
        ChangesetConfigSupport.ChangesetConfig config = RepoFiles.readChangesetConfig(repoRoot);
        if (config.tagStrategy() == ReleaseTagStrategy.PER_MODULE) {
            throw new IllegalStateException("Fresh release metadata cannot infer per-module release targets after changesets are consumed. "
                + "Use the committed release plan manifest or run before applying the plan.");
        }
        return new ReleaseDescriptor(releaseVersion, config.tagStrategy(),
            Collections.singletonList(new ReleasePlan.ReleaseTarget(null, "v" + releaseVersion)));
    }

    public String releaseVersionFromManifest() throws IOException {
        return RepoFiles.readManifestField(repoRoot, "releaseVersion");
    }

    public String readManifestField(String field, boolean fresh) throws IOException, InterruptedException {
        if (!fresh) {
            return RepoFiles.readManifestField(repoRoot, field);
        }
        ReleasePlan plan = plan();
        if (plan.hasPendingChangesets()) {
            JsonNode value = ReleaseJsonUtils.readTree(plan.toJson()).get(field);
            return requiredManifestFieldText(value, field, "fresh release plan");
        }
        JsonNode value = ReleaseJsonUtils.readTree(ReleaseJsonUtils.toPrettyJson(freshAppliedManifest())).get(field);
        return requiredManifestFieldText(value, field, "fresh applied release metadata");
    }

    public String wholeRepoTagFromManifest() throws IOException {
        return "v" + releaseVersionFromManifest();
    }

    public Path releasePlanMarkdownFile() {
        return repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_MD);
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
            java.nio.file.Files.readAllBytes(repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_JSON)),
            StandardCharsets.UTF_8
        ));
    }

    private Map<String, Object> freshAppliedManifest() throws IOException {
        ReleaseDescriptor release;
        try {
            release = descriptorFromFreshPlan();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while generating fresh release metadata", exception);
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("releaseVersion", release.releaseVersion);
        payload.put("nextSnapshotVersion", BuildModelSupport.readRevision(repoRoot));
        payload.put("releaseLevel", null);
        payload.put("tagStrategy", release.tagStrategy.id);
        payload.put("tags", release.tagNames());
        List<Map<String, Object>> renderedTargets = new ArrayList<Map<String, Object>>();
        for (ReleasePlan.ReleaseTarget target : release.releaseTargets) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("module", target.module);
            entry.put("tag", target.tag);
            renderedTargets.add(entry);
        }
        payload.put("releaseTargets", renderedTargets);
        payload.put("changesets", Collections.emptyList());
        return payload;
    }

    private static String requiredText(JsonNode node, String field) {
        String value = textOrNull(node.get(field));
        if (value == null) {
            throw new IllegalStateException("Missing field " + field + " in release manifest");
        }
        return value;
    }

    private static String requiredManifestFieldText(JsonNode value, String field, String source) {
        if (value == null || value.isNull()) {
            throw new IllegalStateException("Missing field `" + field + "` in " + source);
        }
        if (value.isArray()) {
            List<String> values = new ArrayList<String>();
            for (JsonNode item : value) {
                values.add(item.asText());
            }
            return Arrays.toString(values.toArray(new String[0]));
        }
        return value.asText();
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
