package io.github.sonofmagic.javachanges.core.env;

final class AuditOutcome {
    final String message;
    private final boolean failure;

    private AuditOutcome(String message, boolean failure) {
        this.message = message;
        this.failure = failure;
    }

    static AuditOutcome success(String message) {
        return new AuditOutcome(message, false);
    }

    static AuditOutcome failure(String message) {
        return new AuditOutcome(message, true);
    }

    boolean isFailure() {
        return failure;
    }
}
