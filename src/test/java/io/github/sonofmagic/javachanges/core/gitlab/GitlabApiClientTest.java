package io.github.sonofmagic.javachanges.core.gitlab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitlabApiClientTest {

    @Test
    void defaultTimeoutsAreApplied() {
        GitlabApiClient client = new GitlabApiClient();

        assertEquals(30000, client.connectTimeoutMillis());
        assertEquals(30000, client.readTimeoutMillis());
    }

    @Test
    void customTimeoutsAreApplied() {
        GitlabApiClient client = new GitlabApiClient(1000, 2000);

        assertEquals(1000, client.connectTimeoutMillis());
        assertEquals(2000, client.readTimeoutMillis());
    }

    @Test
    void releaseExistsReturnsFalseForNotFound() throws Exception {
        HttpServer server = startServer(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });
        try {
            GitlabApiClient client = new GitlabApiClient(1000, 1000, envFor(server));

            assertFalse(client.releaseExists("123", "v1.2.3"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void emptyErrorResponseIncludesHttpStatus() throws Exception {
        HttpServer server = startServer(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            }
        });
        try {
            GitlabApiClient client = new GitlabApiClient(1000, 1000, envFor(server));

            IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.findOpenMergeRequestIid("123", "release", "main"));

            assertEquals("GitLab API GET /projects/123/merge_requests?state=opened&source_branch=release&target_branch=main failed: HTTP 500",
                error.getMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void authenticatedRemoteUrlEncodesCredentials() {
        Map<String, String> values = new HashMap<String, String>();
        values.put("CI_SERVER_HOST", "gitlab.example.com");
        values.put("CI_PROJECT_PATH", "group/repo");
        values.put("GITLAB_RELEASE_BOT_USERNAME", "release bot");
        values.put("GITLAB_RELEASE_BOT_TOKEN", "tok/en:value");
        GitlabApiClient client = new GitlabApiClient(1000, 1000, mapEnv(values));

        assertEquals("https://release+bot:tok%2Fen%3Avalue@gitlab.example.com/group/repo.git",
            client.authenticatedRemoteUrl());
    }

    private static HttpServer startServer(HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
        return server;
    }

    private static GitlabApiClient.Env envFor(HttpServer server) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("CI_SERVER_URL", "http://127.0.0.1:" + server.getAddress().getPort());
        values.put("CI_SERVER_HOST", "127.0.0.1:" + server.getAddress().getPort());
        values.put("GITLAB_RELEASE_BOT_TOKEN", "token");
        return mapEnv(values);
    }

    private static GitlabApiClient.Env mapEnv(final Map<String, String> values) {
        return new GitlabApiClient.Env() {
            @Override
            public String get(String name) {
                return values.get(name);
            }
        };
    }
}
