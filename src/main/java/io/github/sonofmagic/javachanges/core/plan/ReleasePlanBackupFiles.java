package io.github.sonofmagic.javachanges.core.plan;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import io.github.sonofmagic.javachanges.core.ChangesetPaths;
import io.github.sonofmagic.javachanges.core.ReleaseJsonUtils;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.changeset.Changeset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ReleasePlanBackupFiles {
    private ReleasePlanBackupFiles() {
    }

    static Path backupPath(Path repoRoot) {
        return repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_BACKUP_JSON);
    }

    static Path writeBackup(Path repoRoot, ReleasePlan plan) throws IOException {
        Path backupPath = backupPath(repoRoot);
        Files.createDirectories(backupPath.getParent());
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("version", Integer.valueOf(1));
        payload.put("createdAt", OffsetDateTime.now().toString());
        payload.put("releaseVersion", plan.getReleaseVersion());
        payload.put("files", fileBackups(repoRoot, plan));
        Files.write(backupPath, ReleaseJsonUtils.toPrettyJson(payload).getBytes(StandardCharsets.UTF_8));
        return backupPath;
    }

    static Path restore(Path repoRoot) throws IOException {
        Path backupPath = backupPath(repoRoot);
        if (!Files.exists(backupPath)) {
            throw new IllegalStateException(ReleaseMessages.missingReleasePlanBackup(backupPath));
        }
        JsonNode root = ReleaseJsonUtils.readTree(new String(Files.readAllBytes(backupPath), StandardCharsets.UTF_8));
        JsonNode files = root.get("files");
        if (files == null || !files.isArray()) {
            throw new IllegalStateException(ReleaseMessages.invalidReleasePlanBackup(backupPath));
        }
        for (JsonNode file : files) {
            restoreFile(repoRoot, backupPath, file);
        }
        return backupPath;
    }

    private static List<Map<String, Object>> fileBackups(Path repoRoot, ReleasePlan plan) throws IOException {
        List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();
        for (String relativePath : backupRelativePaths(repoRoot, plan)) {
            Path path = repoRoot.resolve(relativePath);
            boolean existed = Files.exists(path);
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("path", relativePath);
            entry.put("existed", Boolean.valueOf(existed));
            entry.put("content", existed ? new String(Files.readAllBytes(path), StandardCharsets.UTF_8) : null);
            files.add(entry);
        }
        return files;
    }

    private static List<String> backupRelativePaths(Path repoRoot, ReleasePlan plan) {
        Set<String> paths = new LinkedHashSet<String>();
        paths.add(BuildModelSupport.revisionFileLabel(repoRoot));
        paths.add("CHANGELOG.md");
        paths.add(ChangesetPaths.DIR + "/" + ChangesetPaths.RELEASE_PLAN_JSON);
        paths.add(ChangesetPaths.DIR + "/" + ChangesetPaths.RELEASE_PLAN_MD);
        for (Changeset changeset : plan.getChangesets()) {
            paths.add(repoRoot.relativize(changeset.path).toString());
        }
        return new ArrayList<String>(paths);
    }

    private static void restoreFile(Path repoRoot, Path backupPath, JsonNode file) throws IOException {
        JsonNode pathNode = file.get("path");
        JsonNode existedNode = file.get("existed");
        if (pathNode == null || existedNode == null) {
            throw new IllegalStateException(ReleaseMessages.invalidReleasePlanBackup(backupPath));
        }
        String relativePath = pathNode.asText();
        Path target = repoRoot.resolve(relativePath).normalize();
        if (!target.startsWith(repoRoot.normalize())) {
            throw new IllegalStateException(ReleaseMessages.outputPathMustStayInsideRepository("backup path", relativePath));
        }
        if (existedNode.asBoolean()) {
            Files.createDirectories(target.getParent());
            JsonNode contentNode = file.get("content");
            Files.write(target, (contentNode == null ? "" : contentNode.asText()).getBytes(StandardCharsets.UTF_8));
            return;
        }
        Files.deleteIfExists(target);
    }
}
