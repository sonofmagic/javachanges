package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class GitlabReleaseSupport {
    private final Path repoRoot;
    private final PrintStream out;

    GitlabReleaseSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
    }

    void planMergeRequest(GitlabReleasePlanRequest request) throws IOException, InterruptedException {
        if (trimToNull(request.projectId) == null) {
            throw new IllegalArgumentException("Missing GitLab project id. Pass --project-id or set CI_PROJECT_ID.");
        }

        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        if (!plan.hasPendingChangesets()) {
            out.println("No pending changesets. Skip release MR.");
            return;
        }

        String releaseVersion = plan.getReleaseVersion();
        String releaseBranch = request.releaseBranch;
        String targetBranch = request.targetBranch;
        String commitMessage = "chore(release): apply changesets for v" + releaseVersion;
        String title = "chore(release): release v" + releaseVersion;

        out.println("Release branch: " + releaseBranch);
        out.println("Target branch: " + targetBranch);
        out.println("Release version: " + releaseVersion);

        if (!request.execute) {
            out.println("Dry-run only. Use --execute true to create/update the GitLab MR.");
            return;
        }

        configureBotIdentity();
        runGit("checkout", "-B", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan);
        String description = new String(
            Files.readAllBytes(repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_MD)),
            StandardCharsets.UTF_8
        );
        runGit("add", "pom.xml", "CHANGELOG.md", CHANGESETS_DIR);
        if (hasNoStagedChanges()) {
            out.println("No staged release plan changes. Skip release MR update.");
            return;
        }
        runGit("commit", "-m", commitMessage);
        runGit("push", "--force-with-lease", authenticatedRemoteUrl(), "HEAD:" + releaseBranch);

        Integer mergeRequestIid = findOpenMergeRequestIid(request.projectId, releaseBranch, targetBranch);
        if (mergeRequestIid == null) {
            String response = gitlabApi(
                "POST",
                "/projects/" + request.projectId + "/merge_requests",
                formBody(
                    "source_branch", releaseBranch,
                    "target_branch", targetBranch,
                    "title", title,
                    "description", description,
                    "remove_source_branch", "true"
                )
            );
            out.println("Created GitLab MR !" + requiredJsonInt(response, "iid"));
            return;
        }

        gitlabApi(
            "PUT",
            "/projects/" + request.projectId + "/merge_requests/" + mergeRequestIid,
            formBody(
                "title", title,
                "description", description,
                "remove_source_branch", "true"
            )
        );
        out.println("Updated GitLab MR !" + mergeRequestIid);
    }

    void tagFromReleasePlan(GitlabTagRequest request) throws IOException, InterruptedException {
        String beforeSha = trimToNull(request.beforeSha);
        String currentSha = trimToNull(request.currentSha);
        if (beforeSha == null || currentSha == null || beforeSha.matches("0+")) {
            out.println("Missing previous SHA. Skip release tag.");
            return;
        }

        if (!changedBetween(beforeSha, currentSha, CHANGESETS_DIR + "/" + RELEASE_PLAN_JSON)) {
            out.println("No release plan manifest change detected. Skip release tag.");
            return;
        }

        String releaseVersion = RepoFiles.readManifestField(repoRoot, "releaseVersion");
        String tagName = "v" + releaseVersion;
        out.println("Release tag: " + tagName);

        if (remoteTagExists(tagName)) {
            out.println("Tag already exists remotely. Skip.");
            return;
        }

        if (!request.execute) {
            out.println("Dry-run only. Use --execute true to create and push the release tag.");
            return;
        }

        runGit("tag", tagName, currentSha);
        runGit("push", authenticatedRemoteUrl(), "refs/tags/" + tagName);
        out.println("Created and pushed tag " + tagName);
    }

    private void configureBotIdentity() throws IOException, InterruptedException {
        String botUsername = requireEnv("GITLAB_RELEASE_BOT_USERNAME");
        String gitlabHost = firstNonBlank(System.getenv("CI_SERVER_HOST"), "gitlab.example.com");
        runGit("config", "user.name", "gitlab-release-bot");
        runGit("config", "user.email", botUsername + "@users.noreply." + gitlabHost);
    }

    private boolean hasNoStagedChanges() throws IOException, InterruptedException {
        return runGitAllowFailure("diff", "--cached", "--quiet") == 0;
    }

    private boolean changedBetween(String beforeSha, String currentSha, String path) throws IOException, InterruptedException {
        return runGitAllowFailure("diff", "--quiet", beforeSha, currentSha, "--", path) != 0;
    }

    private boolean remoteTagExists(String tagName) throws IOException, InterruptedException {
        CommandResult result = runGitCapture("ls-remote", "--tags", authenticatedRemoteUrl(), "refs/tags/" + tagName);
        return result.exitCode == 0 && trimToNull(result.stdoutText()) != null;
    }

    private Integer findOpenMergeRequestIid(String projectId, String sourceBranch, String targetBranch) throws IOException {
        String response = gitlabApi(
            "GET",
            "/projects/" + projectId + "/merge_requests?state=opened&source_branch="
                + urlEncode(sourceBranch) + "&target_branch=" + urlEncode(targetBranch),
            null
        );
        Matcher matcher = Pattern.compile("\"iid\"\\s*:\\s*(\\d+)").matcher(response);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group(1));
    }

    private String gitlabApi(String method, String path, String body) throws IOException {
        String serverUrl = firstNonBlank(System.getenv("CI_SERVER_URL"), "https://" + requireEnv("CI_SERVER_HOST"));
        URL url = new URL(serverUrl + "/api/v4" + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("PRIVATE-TOKEN", requireEnv("GITLAB_RELEASE_BOT_TOKEN"));
        connection.setRequestProperty("Accept", "application/json");
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            OutputStream output = connection.getOutputStream();
            output.write(bytes);
            output.close();
        }
        byte[] responseBytes = readAllBytes(connection.getResponseCode() >= 400
            ? connection.getErrorStream()
            : connection.getInputStream());
        String response = new String(responseBytes, StandardCharsets.UTF_8);
        if (connection.getResponseCode() >= 400) {
            throw new IllegalStateException("GitLab API " + method + " " + path + " failed: " + response);
        }
        return response;
    }

    private String authenticatedRemoteUrl() {
        String host = requireEnv("CI_SERVER_HOST");
        String projectPath = requireEnv("CI_PROJECT_PATH");
        return "https://" + urlEncode(requireEnv("GITLAB_RELEASE_BOT_USERNAME"))
            + ":" + urlEncode(requireEnv("GITLAB_RELEASE_BOT_TOKEN"))
            + "@" + host + "/" + projectPath + ".git";
    }

    private String formBody(String... keyValues) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(urlEncode(keyValues[i])).append('=').append(urlEncode(keyValues[i + 1]));
        }
        return builder.toString();
    }

    private int requiredJsonInt(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing `" + field + "` in GitLab response: " + json);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode URL component", exception);
        }
    }

    private void runGit(String... args) throws IOException, InterruptedException {
        CommandResult result = runGitCapture(args);
        if (result.exitCode != 0) {
            String error = trimToNull(result.stderrText());
            throw new IllegalStateException(error == null ? "git command failed: " + Arrays.asList(args) : error);
        }
    }

    private int runGitAllowFailure(String... args) throws IOException, InterruptedException {
        return runGitCapture(args).exitCode;
    }

    private CommandResult runGitCapture(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add("git");
        command.addAll(Arrays.asList(args));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoRoot.toFile());
        Process process = builder.start();
        byte[] stdout = readAllBytes(process.getInputStream());
        byte[] stderr = readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout, stderr);
    }
}
