package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReleaseTextUtilsTest {

    @Test
    void redactSensitiveTextMasksUrlCredentials() {
        String text = "fatal: unable to access 'https://bot:supersecret@example.com/group/repo.git/'";

        String redacted = ReleaseTextUtils.redactSensitiveText(text);

        assertEquals("fatal: unable to access 'https://****@example.com/group/repo.git/'", redacted);
    }
}
