package io.github.sonofmagic.javachanges.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PublishDoctorSupport {
    private final Path repoRoot;
    private final PrintStream out;

    public PublishDoctorSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
    }

    public boolean doctor(PublishDoctorRequest request) throws IOException, InterruptedException {
        PublishDoctorReport report = inspect(request);
        if (request.format == OutputFormat.JSON) {
            out.println(report.toJson());
            return report.ok();
        }
        printText(report);
        return report.ok();
    }

    private PublishDoctorReport inspect(PublishDoctorRequest request) throws IOException, InterruptedException {
        PublishDoctorReport report = new PublishDoctorReport(request.target);
        BuildModelSupport.BuildModel model = BuildModelSupport.detect(repoRoot);
        if (model == null) {
            report.failed("Project", "build model", "No supported Maven or Gradle build model was found.");
            report.nextCommands.add("javachanges init --directory " + repoRoot);
            return report;
        }

        report.buildTool = model.type == BuildModelSupport.BuildType.MAVEN ? "maven" : "gradle";
        if (model.type != BuildModelSupport.BuildType.MAVEN) {
            report.failed("Project", "build tool", "doctor-publish currently validates Maven publishing only.");
            report.nextCommands.add("javachanges gradle-publish --directory " + repoRoot + " --snapshot");
            return report;
        }
        report.ok("Project", "build tool", "Maven project detected.");

        report.currentRevision = BuildModelSupport.readRevision(repoRoot);
        report.mode = resolveMode(request.mode, report.currentRevision);
        report.publishVersion = "snapshot".equals(report.mode)
            ? report.currentRevision
            : ReleaseTextUtils.stripSnapshot(report.currentRevision);

        checkVersion(report);
        MavenPom pom = MavenPom.read(model.versionFile);
        checkCoordinates(report, pom);
        checkMetadata(report, pom);
        checkMavenCommand(report);
        checkCentralProfiles(report, pom);
        checkModeProfile(report, pom);
        checkCredentials(report);

        report.nextCommands.add("javachanges preflight --directory " + repoRoot
            + ("snapshot".equals(report.mode) ? " --snapshot" : " --tag v" + report.publishVersion));
        report.nextCommands.add("javachanges publish --directory " + repoRoot
            + ("snapshot".equals(report.mode) ? " --snapshot" : " --tag v" + report.publishVersion)
            + " --execute true");
        return report;
    }

    private static String resolveMode(String configuredMode, String revision) {
        if ("auto".equals(configuredMode)) {
            return revision.endsWith("-SNAPSHOT") ? "snapshot" : "release";
        }
        return configuredMode;
    }

    private void checkVersion(PublishDoctorReport report) {
        if ("snapshot".equals(report.mode) && !report.currentRevision.endsWith("-SNAPSHOT")) {
            report.failed("Project", "snapshot version", "Snapshot publishing requires the current revision to end with -SNAPSHOT.");
            return;
        }
        report.ok("Project", "version", "Current revision is " + report.currentRevision + "; publish mode is " + report.mode + ".");
    }

    private void checkCoordinates(PublishDoctorReport report, MavenPom pom) {
        Set<String> missing = new LinkedHashSet<String>();
        if (pom.groupId() == null) {
            missing.add("groupId");
        }
        if (pom.artifactId() == null) {
            missing.add("artifactId");
        }
        if (pom.version() == null) {
            missing.add("version");
        }
        if (missing.isEmpty()) {
            report.ok("Maven POM", "coordinates", pom.groupId() + ":" + pom.artifactId() + ":" + pom.version());
            return;
        }
        report.failed("Maven POM", "coordinates", "Missing " + join(missing) + ".");
    }

    private void checkMetadata(PublishDoctorReport report, MavenPom pom) {
        Set<String> missing = new LinkedHashSet<String>();
        requireDirectText(pom, missing, "name");
        requireDirectText(pom, missing, "description");
        requireDirectText(pom, missing, "url");
        if (pom.firstDescendant("licenses", "license", "name") == null
            || pom.firstDescendant("licenses", "license", "url") == null) {
            missing.add("licenses/license name+url");
        }
        if (pom.firstDescendant("developers", "developer", "id") == null
            && pom.firstDescendant("developers", "developer", "name") == null) {
            missing.add("developers/developer id or name");
        }
        if (pom.firstDescendant("scm", "url") == null
            || (pom.firstDescendant("scm", "connection") == null
            && pom.firstDescendant("scm", "developerConnection") == null)) {
            missing.add("scm url+connection");
        }
        if (missing.isEmpty()) {
            report.ok("Maven POM", "Central metadata", "Required Maven Central metadata is present.");
            return;
        }
        report.failed("Maven POM", "Central metadata", "Missing " + join(missing) + ".");
    }

    private void checkMavenCommand(PublishDoctorReport report) throws IOException, InterruptedException {
        MavenCommand command = ReleaseProcessUtils.resolveMavenCommand(repoRoot);
        if (command == null) {
            report.failed("Runtime", "Maven command", ReleaseMessages.noMavenCommandFound());
            return;
        }
        report.ok("Runtime", "Maven command", command.command + " (" + command.source + ")");
    }

    private void checkCentralProfiles(PublishDoctorReport report, MavenPom pom) {
        checkProfile(report, pom, "central-publish");
        checkProfile(report, pom, "central-snapshot-publish");
    }

    private void checkModeProfile(PublishDoctorReport report, MavenPom pom) {
        String profile = "snapshot".equals(report.mode) ? "central-snapshot-publish" : "central-publish";
        checkProfilePlugin(report, pom, profile, "flatten-maven-plugin");
        checkProfilePlugin(report, pom, profile, "maven-source-plugin");
        checkProfilePlugin(report, pom, profile, "maven-javadoc-plugin");
        checkProfilePlugin(report, pom, profile, "maven-gpg-plugin");
        checkProfilePlugin(report, pom, profile, "central-publishing-maven-plugin");
    }

    private void checkCredentials(PublishDoctorReport report) throws IOException, InterruptedException {
        if ("snapshot".equals(report.mode)) {
            checkEnvAny(report, "Credentials", "snapshot repository URL",
                "MAVEN_SNAPSHOT_REPOSITORY_URL");
            checkEnvPair(report, "Credentials", "snapshot repository credentials",
                new String[][]{
                    {"MAVEN_CENTRAL_USERNAME", "MAVEN_CENTRAL_PASSWORD"},
                    {"MAVEN_SNAPSHOT_REPOSITORY_USERNAME", "MAVEN_SNAPSHOT_REPOSITORY_PASSWORD"},
                    {"MAVEN_REPOSITORY_USERNAME", "MAVEN_REPOSITORY_PASSWORD"}
                });
        } else {
            checkEnvAny(report, "Credentials", "release repository URL",
                "MAVEN_RELEASE_REPOSITORY_URL");
            checkEnvPair(report, "Credentials", "release repository credentials",
                new String[][]{
                    {"MAVEN_CENTRAL_USERNAME", "MAVEN_CENTRAL_PASSWORD"},
                    {"MAVEN_RELEASE_REPOSITORY_USERNAME", "MAVEN_RELEASE_REPOSITORY_PASSWORD"},
                    {"MAVEN_REPOSITORY_USERNAME", "MAVEN_REPOSITORY_PASSWORD"}
                });
        }
        checkSigning(report);
    }

    private void checkSigning(PublishDoctorReport report) throws IOException, InterruptedException {
        boolean hasPrivateKeyEnv = ReleaseTextUtils.trimToNull(System.getenv("MAVEN_GPG_PRIVATE_KEY")) != null;
        boolean hasPassphrase = ReleaseTextUtils.trimToNull(System.getenv("MAVEN_GPG_PASSPHRASE")) != null;
        if (hasPrivateKeyEnv && hasPassphrase) {
            report.ok("Credentials", "GPG signing", "MAVEN_GPG_PRIVATE_KEY and MAVEN_GPG_PASSPHRASE are set.");
            return;
        }
        CommandResult result;
        try {
            result = ReleaseProcessUtils.runCapture(repoRoot, "gpg", "--list-secret-keys");
        } catch (IOException exception) {
            result = null;
        }
        if (result != null && result.exitCode == 0 && ReleaseTextUtils.trimToNull(result.stdoutText()) != null) {
            report.ok("Credentials", "GPG signing", "A local GPG secret key is available.");
            return;
        }
        report.failed("Credentials", "GPG signing",
            "Set MAVEN_GPG_PRIVATE_KEY and MAVEN_GPG_PASSPHRASE, or make a local GPG secret key available.");
    }

    private void checkProfile(PublishDoctorReport report, MavenPom pom, String profileId) {
        if (pom.profile(profileId) == null) {
            report.failed("Maven POM", "profile " + profileId, "Profile is missing.");
            return;
        }
        report.ok("Maven POM", "profile " + profileId, "Profile is present.");
    }

    private void checkProfilePlugin(PublishDoctorReport report, MavenPom pom, String profileId, String artifactId) {
        if (pom.hasProfilePlugin(profileId, artifactId)) {
            report.ok("Maven POM", profileId + " " + artifactId, "Plugin is configured.");
            return;
        }
        report.failed("Maven POM", profileId + " " + artifactId, "Plugin is missing.");
    }

    private void checkEnvAny(PublishDoctorReport report, String section, String name, String variable) {
        if (ReleaseTextUtils.trimToNull(System.getenv(variable)) != null) {
            report.ok(section, name, variable + " is set.");
            return;
        }
        report.failed(section, name, "Missing " + variable + ".");
    }

    private void checkEnvPair(PublishDoctorReport report, String section, String name, String[][] alternatives) {
        for (String[] pair : alternatives) {
            if (ReleaseTextUtils.trimToNull(System.getenv(pair[0])) != null
                && ReleaseTextUtils.trimToNull(System.getenv(pair[1])) != null) {
                report.ok(section, name, pair[0] + "/" + pair[1] + " are set.");
                return;
            }
        }
        StringBuilder message = new StringBuilder("Set one of: ");
        for (int i = 0; i < alternatives.length; i++) {
            if (i > 0) {
                message.append(", ");
            }
            message.append(alternatives[i][0]).append("/").append(alternatives[i][1]);
        }
        report.failed(section, name, message.toString() + ".");
    }

    private void printText(PublishDoctorReport report) {
        out.println("Publish readiness: " + (report.ok() ? "PASSED" : "FAILED"));
        out.println("Target: " + report.target);
        if (report.mode != null) {
            out.println("Mode: " + report.mode);
        }
        if (report.currentRevision != null) {
            out.println("Current revision: " + report.currentRevision);
        }
        if (report.publishVersion != null) {
            out.println("Publish version: " + report.publishVersion);
        }
        out.println();
        out.println("Checks:");
        for (PublishDoctorReport.Check check : report.checks) {
            out.println("- [" + check.status + "] " + check.section + " / " + check.name + ": " + check.message);
        }
        if (!report.nextCommands.isEmpty()) {
            out.println();
            out.println("Next steps:");
            for (String command : report.nextCommands) {
                out.println("  " + command);
            }
        }
    }

    private static void requireDirectText(MavenPom pom, Set<String> missing, String name) {
        if (pom.directText(name) == null) {
            missing.add(name);
        }
    }

    private static String join(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String value : values) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(value);
            index++;
        }
        return builder.toString();
    }

    private static final class MavenPom {
        private final Document document;

        private MavenPom(Document document) {
            this.document = document;
        }

        static MavenPom read(Path pomPath) throws IOException {
            try (InputStream inputStream = Files.newInputStream(pomPath)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                return new MavenPom(builder.parse(inputStream));
            } catch (ParserConfigurationException exception) {
                throw new IllegalStateException(ReleaseMessages.failedToConfigureXmlParser(pomPath), exception);
            } catch (SAXException exception) {
                throw new IllegalStateException(ReleaseMessages.failedToParsePom(pomPath), exception);
            }
        }

        String groupId() {
            String value = directText("groupId");
            return value == null ? firstDescendant("parent", "groupId") : value;
        }

        String artifactId() {
            return directText("artifactId");
        }

        String version() {
            String value = directText("version");
            return value == null ? firstDescendant("parent", "version") : value;
        }

        String directText(String name) {
            return childText(document.getDocumentElement(), name);
        }

        Element profile(String profileId) {
            Element profiles = child(document.getDocumentElement(), "profiles");
            if (profiles == null) {
                return null;
            }
            for (Element profile : children(profiles, "profile")) {
                if (profileId.equals(childText(profile, "id"))) {
                    return profile;
                }
            }
            return null;
        }

        boolean hasProfilePlugin(String profileId, String artifactId) {
            Element profile = profile(profileId);
            if (profile == null) {
                return false;
            }
            Element build = child(profile, "build");
            if (build == null) {
                return false;
            }
            Element plugins = child(build, "plugins");
            if (plugins == null) {
                return false;
            }
            for (Element plugin : children(plugins, "plugin")) {
                if (artifactId.equals(childText(plugin, "artifactId"))) {
                    return true;
                }
            }
            return false;
        }

        String firstDescendant(String... path) {
            Element current = document.getDocumentElement();
            for (String name : path) {
                current = child(current, name);
                if (current == null) {
                    return null;
                }
            }
            return ReleaseTextUtils.trimToNull(current.getTextContent());
        }

        private static Element child(Element parent, String name) {
            if (parent == null) {
                return null;
            }
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                if (matches(element, name)) {
                    return element;
                }
            }
            return null;
        }

        private static Iterable<Element> children(Element parent, String name) {
            java.util.List<Element> elements = new java.util.ArrayList<Element>();
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                if (matches(element, name)) {
                    elements.add(element);
                }
            }
            return elements;
        }

        private static String childText(Element parent, String name) {
            Element child = child(parent, name);
            return child == null ? null : ReleaseTextUtils.trimToNull(child.getTextContent());
        }

        private static boolean matches(Element element, String name) {
            return name.equals(element.getLocalName()) || name.equals(element.getNodeName());
        }
    }
}
