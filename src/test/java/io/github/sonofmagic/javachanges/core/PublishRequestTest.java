package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublishRequestTest {

    @Test
    void fromOptionsReadsExplicitSnapshotBuildStamp() {
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("snapshot-build-stamp", "20260420.154500.ci001");

        PublishRequest request = PublishRequest.fromOptions(options, true);

        assertEquals("20260420.154500.ci001", request.snapshotBuildStamp);
    }
}
