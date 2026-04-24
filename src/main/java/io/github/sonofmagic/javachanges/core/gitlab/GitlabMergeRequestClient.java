package io.github.sonofmagic.javachanges.core.gitlab;

import java.io.IOException;

public interface GitlabMergeRequestClient {
    Integer findOpenMergeRequestIid(String projectId, String sourceBranch, String targetBranch) throws IOException;

    String createMergeRequest(String projectId, String sourceBranch, String targetBranch,
                              String title, String description) throws IOException;

    void updateMergeRequest(String projectId, int mergeRequestIid, String title, String description) throws IOException;

    String authenticatedRemoteUrl();

    int requiredJsonInt(String json, String field);

    boolean releaseExists(String projectId, String tagName) throws IOException;

    void createRelease(String projectId, String tagName, String releaseName, String description) throws IOException;

    void updateRelease(String projectId, String tagName, String releaseName, String description) throws IOException;
}
