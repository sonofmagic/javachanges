package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstNonBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.readAllBytes;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.requireEnv;

final class GitlabApiClient {
    Integer findOpenMergeRequestIid(String projectId, String sourceBranch, String targetBranch) throws IOException {
        String response = request(
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

    String createMergeRequest(String projectId, String sourceBranch, String targetBranch,
                              String title, String description) throws IOException {
        return request(
            "POST",
            "/projects/" + projectId + "/merge_requests",
            formBody(
                "source_branch", sourceBranch,
                "target_branch", targetBranch,
                "title", title,
                "description", description,
                "remove_source_branch", "true"
            )
        );
    }

    void updateMergeRequest(String projectId, int mergeRequestIid, String title, String description) throws IOException {
        request(
            "PUT",
            "/projects/" + projectId + "/merge_requests/" + mergeRequestIid,
            formBody(
                "title", title,
                "description", description,
                "remove_source_branch", "true"
            )
        );
    }

    String authenticatedRemoteUrl() {
        String host = requireEnv("CI_SERVER_HOST");
        String projectPath = requireEnv("CI_PROJECT_PATH");
        return "https://" + urlEncode(requireEnv("GITLAB_RELEASE_BOT_USERNAME"))
            + ":" + urlEncode(requireEnv("GITLAB_RELEASE_BOT_TOKEN"))
            + "@" + host + "/" + projectPath + ".git";
    }

    int requiredJsonInt(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing `" + field + "` in GitLab response: " + json);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String request(String method, String path, String body) throws IOException {
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

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode URL component", exception);
        }
    }
}
