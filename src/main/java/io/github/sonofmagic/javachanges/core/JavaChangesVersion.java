package io.github.sonofmagic.javachanges.core;

public final class JavaChangesVersion {
    static final String FALLBACK_RELEASED_VERSION = "1.7.0";

    private JavaChangesVersion() {
    }

    public static String releasedVersion(String explicitVersion) {
        String explicit = ReleaseTextUtils.trimToNull(explicitVersion);
        if (explicit != null) {
            return explicit;
        }
        String implementationVersion =
            ReleaseTextUtils.trimToNull(JavaChangesCli.class.getPackage().getImplementationVersion());
        if (implementationVersion != null) {
            return implementationVersion.endsWith("-SNAPSHOT")
                ? implementationVersion.substring(0, implementationVersion.length() - "-SNAPSHOT".length())
                : implementationVersion;
        }
        return FALLBACK_RELEASED_VERSION;
    }
}
