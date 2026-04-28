package io.github.sonofmagic.javachanges.core.gitlab;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.sonofmagic.javachanges.core.ReleaseJsonUtils;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GitlabApiClient implements GitlabMergeRequestClient {
    private static final int DEFAULT_TIMEOUT_MILLIS = 30000;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final Env env;

    public GitlabApiClient() {
        this(DEFAULT_TIMEOUT_MILLIS, DEFAULT_TIMEOUT_MILLIS, new Env() {
            @Override
            public String get(String name) {
                return System.getenv(name);
            }
        });
    }

    GitlabApiClient(int connectTimeoutMillis, int readTimeoutMillis) {
        this(connectTimeoutMillis, readTimeoutMillis, new Env() {
            @Override
            public String get(String name) {
                return System.getenv(name);
            }
        });
    }

    GitlabApiClient(int connectTimeoutMillis, int readTimeoutMillis, Env env) {
        if (connectTimeoutMillis < 0) {
            throw new IllegalArgumentException("connectTimeoutMillis must be 0 or greater");
        }
        if (readTimeoutMillis < 0) {
            throw new IllegalArgumentException("readTimeoutMillis must be 0 or greater");
        }
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.env = env;
    }

    public Integer findOpenMergeRequestIid(String projectId, String sourceBranch, String targetBranch) throws IOException {
        String response = request(
            "GET",
            "/projects/" + projectId + "/merge_requests?state=opened&source_branch="
                + urlEncode(sourceBranch) + "&target_branch=" + urlEncode(targetBranch),
            null
        );
        JsonNode root = ReleaseJsonUtils.readTree(response);
        if (!root.isArray() || root.size() == 0) {
            return null;
        }
        JsonNode first = root.get(0);
        return first != null && first.hasNonNull("iid") ? Integer.valueOf(first.get("iid").asInt()) : null;
    }

    public String createMergeRequest(String projectId, String sourceBranch, String targetBranch,
                                     String title, String description) throws IOException {
        return request(
            "POST",
            "/projects/" + projectId + "/merge_requests",
            formBody(orderedMap(
                "source_branch", sourceBranch,
                "target_branch", targetBranch,
                "title", title,
                "description", description,
                "remove_source_branch", "true"
            ))
        );
    }

    public void updateMergeRequest(String projectId, int mergeRequestIid, String title, String description) throws IOException {
        request(
            "PUT",
            "/projects/" + projectId + "/merge_requests/" + mergeRequestIid,
            formBody(orderedMap(
                "title", title,
                "description", description,
                "remove_source_branch", "true"
            ))
        );
    }

    public String authenticatedRemoteUrl() {
        String host = requireEnv("CI_SERVER_HOST");
        String projectPath = requireEnv("CI_PROJECT_PATH");
        return "https://" + urlEncode(requireEnv("GITLAB_RELEASE_BOT_USERNAME"))
            + ":" + urlEncode(requireEnv("GITLAB_RELEASE_BOT_TOKEN"))
            + "@" + host + "/" + projectPath + ".git";
    }

    public int requiredJsonInt(String json, String field) {
        JsonNode root = ReleaseJsonUtils.readTree(json);
        JsonNode value = root.get(field);
        if (value == null || value.isNull() || !value.canConvertToInt()) {
            throw new IllegalStateException("Missing `" + field + "` in GitLab response: " + json);
        }
        return value.asInt();
    }

    public boolean releaseExists(String projectId, String tagName) throws IOException {
        return requestAllowNotFound("GET", "/projects/" + projectId + "/releases/" + urlEncode(tagName), null) != null;
    }

    public void createRelease(String projectId, String tagName, String releaseName, String description) throws IOException {
        request(
            "POST",
            "/projects/" + projectId + "/releases",
            formBody(orderedMap(
                "name", releaseName,
                "tag_name", tagName,
                "description", description,
                "ref", tagName
            ))
        );
    }

    public void updateRelease(String projectId, String tagName, String releaseName, String description) throws IOException {
        request(
            "PUT",
            "/projects/" + projectId + "/releases/" + urlEncode(tagName),
            formBody(orderedMap(
                "name", releaseName,
                "description", description
            ))
        );
    }

    private String request(String method, String path, String body) throws IOException {
        String response = requestAllowNotFound(method, path, body);
        if (response == null) {
            throw new IllegalStateException("GitLab API " + method + " " + path + " failed: Not found");
        }
        return response;
    }

    private String requestAllowNotFound(String method, String path, String body) throws IOException {
        String serverUrl = ReleaseTextUtils.firstNonBlank(env.get("CI_SERVER_URL"),
            "https://" + requireEnv("CI_SERVER_HOST"));
        URL url = new URL(serverUrl + "/api/v4" + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setRequestProperty("PRIVATE-TOKEN", requireEnv("GITLAB_RELEASE_BOT_TOKEN"));
        connection.setRequestProperty("Accept", "application/json");
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            OutputStream output = connection.getOutputStream();
            try {
                output.write(bytes);
            } finally {
                output.close();
            }
        }
        int responseCode = connection.getResponseCode();
        InputStream responseStream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        byte[] responseBytes;
        try {
            responseBytes = responseStream == null ? new byte[0] : ReleaseProcessUtils.readAllBytes(responseStream);
        } finally {
            if (responseStream != null) {
                responseStream.close();
            }
        }
        String response = new String(responseBytes, StandardCharsets.UTF_8);
        if (responseCode == 404) {
            return null;
        }
        if (responseCode >= 400) {
            String detail = ReleaseTextUtils.trimToNull(response);
            if (detail == null) {
                detail = "HTTP " + responseCode;
            }
            throw new IllegalStateException("GitLab API " + method + " " + path + " failed: " + detail);
        }
        return response;
    }

    int connectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    int readTimeoutMillis() {
        return readTimeoutMillis;
    }

    private String requireEnv(String name) {
        String value = ReleaseTextUtils.trimToNull(env.get(name));
        if (value == null) {
            throw new IllegalStateException("缺少环境变量: " + name);
        }
        return value;
    }

    interface Env {
        String get(String name);
    }

    private String formBody(Map<String, String> fields) {
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = fields.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private Map<String, String> orderedMap(String... keyValues) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put(keyValues[i], keyValues[i + 1]);
        }
        return result;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode URL component", exception);
        }
    }
}
