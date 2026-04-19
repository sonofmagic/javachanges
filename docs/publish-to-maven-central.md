# javachanges Publish To Maven Central Guide


## 1. Overview

This repository now includes the core Maven configuration required for Maven Central publishing:

| Capability | Meaning |
| --- | --- |
| `sources.jar` | Attached through `maven-source-plugin` |
| `javadoc.jar` | Attached through `maven-javadoc-plugin` |
| GPG signatures | Added by `maven-gpg-plugin` during `verify` |
| Central upload | Handled by `central-publishing-maven-plugin` during `deploy` |
| Executable CLI jar | `Main-Class` written by `maven-jar-plugin` |

The publishing flow is isolated behind the Maven profile:

```bash
-Pcentral-publish
```

That keeps everyday local builds separate from real Central publishing.

## 2. Prerequisites

Before a real Maven Central release, make sure all of these are ready:

| Item | Required state |
| --- | --- |
| Namespace | Verified in Sonatype Central Portal |
| Portal token | Generated and stored locally or in CI secrets |
| GPG key | Created and available on the publishing machine |
| Version | Must not end with `-SNAPSHOT` |
| Git tag | Recommended for the release version |

> Note: never commit Portal tokens, GPG private keys, or passphrases into the repository.

## 3. Configure `settings.xml`

### 3.1 Default server id

The default Central server id in this repository is:

```xml
<id>central</id>
```

That maps to the `pom.xml` property:

```xml
<central.publishing.serverId>central</central.publishing.serverId>
```

### 3.2 Recommended config

Store your Sonatype Central Portal token in `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>your portal token username</username>
      <password>your portal token password</password>
    </server>
  </servers>
</settings>
```

If you prefer another server id, override it during publish:

```bash
mvn -Pcentral-publish -Dcentral.publishing.serverId=your-server-id deploy
```

The `<id>` inside `settings.xml` must match.

## 4. Configure GPG

### 4.1 Generate a key

```bash
gpg --full-generate-key
```

### 4.2 Inspect keys

```bash
gpg --list-secret-keys --keyid-format LONG
```

### 4.3 Warm up `gpg-agent`

```bash
echo "test" | gpg --clearsign
```

> Tip: prompts or keychain dialogs during signing are expected. Maven Central requires `.asc` signatures for published files.

## 5. Switch to a release version

If the repository currently has:

```xml
<revision>1.0.0-SNAPSHOT</revision>
```

change it to a real release version first:

```xml
<revision>1.0.0</revision>
```

After the release, move it forward again, for example:

```xml
<revision>1.0.1-SNAPSHOT</revision>
```

## 6. Local preflight

Before `deploy`, validate the publishing build:

```bash
mvn -Pcentral-publish -Dgpg.skip=true verify
```

This checks:

| Check | Meaning |
| --- | --- |
| Main jar | Packaging works |
| `sources.jar` | Sources are attached |
| `javadoc.jar` | Javadocs are attached |
| Publish profile | Publishing configuration is wired correctly |

`-Dgpg.skip=true` is only for validating the build chain before real signing.

## 7. First manual release

### 7.1 Recommended first step

For the first release, it is safer to upload and validate first instead of immediately auto-publishing.

The current defaults in `pom.xml` are:

| Parameter | Default |
| --- | --- |
| `central.autoPublish` | `false` |
| `central.waitUntil` | `validated` |

So the normal behavior is:

1. upload the bundle to Central Portal
2. wait for validation
3. publish manually in the portal once validation passes

### 7.2 Publish command

```bash
mvn -Pcentral-publish clean deploy
```

If your GPG key requires a passphrase, Maven will ask for it.

## 8. Auto-publish

Once the first manual release has proven stable, you can auto-publish:

```bash
mvn -Pcentral-publish \
  -Dcentral.autoPublish=true \
  -Dcentral.waitUntil=published \
  clean deploy
```

That uploads, validates, publishes, and waits until the deployment reaches `published`.

## 9. Recommended release sequence

1. make sure the worktree is clean: `git status`
2. change `revision` to the real release version, for example `1.0.0`
3. run `mvn -Pcentral-publish -Dgpg.skip=true verify`
4. confirm GPG works, then run `mvn -Pcentral-publish clean deploy`
5. inspect the deployment in Sonatype Central Portal
6. if you are still using manual publish mode, click publish in the portal
7. create a git tag such as `v1.0.0`
8. move the version to the next snapshot, such as `1.0.1-SNAPSHOT`

## 10. Verify the result

After publishing, check:

| Type | Location |
| --- | --- |
| Central Portal | `https://central.sonatype.com` |
| Maven Central artifact page | `https://central.sonatype.com/artifact/io.github.sonofmagic/javachanges` |

You can also verify dependency resolution in a small sample project:

```xml
<dependency>
  <groupId>io.github.sonofmagic</groupId>
  <artifactId>javachanges</artifactId>
  <version>1.0.0</version>
</dependency>
```

And since this project produces an executable jar, you can also test:

```bash
java -jar javachanges-1.0.0.jar
```

## 11. FAQ

### 11.1 Missing `sources.jar` or `javadoc.jar`

Cause: the `central-publish` profile was not enabled, or the packaging chain is broken.

Fix:

```bash
mvn -Pcentral-publish -Dgpg.skip=true verify
```

### 11.2 Missing signatures

Cause: GPG is not configured correctly, or `gpg-agent` has not cached the passphrase.

Fix:

1. run `gpg --list-secret-keys --keyid-format LONG`
2. run `echo "test" | gpg --clearsign`
3. rerun the Maven publish command

### 11.3 Authentication failures

Cause: the token in `settings.xml` is wrong, or the server id does not match.

Fix:

1. inspect the `<id>` in `~/.m2/settings.xml`
2. inspect `central.publishing.serverId` in `pom.xml`
3. confirm the token has not expired or been revoked

## 12. Summary

The publishing commands for this repository reduce to:

| Goal | Command |
| --- | --- |
| Pre-publish verification | `mvn -Pcentral-publish -Dgpg.skip=true verify` |
| Real publish | `mvn -Pcentral-publish clean deploy` |

## 13. References

| Resource | Link |
| --- | --- |
| Sonatype Central Portal Maven publishing | https://central.sonatype.org/publish/publish-portal-maven/ |
| Sonatype publishing requirements | https://central.sonatype.org/publish/requirements/ |
| Sonatype GPG requirements | https://central.sonatype.org/publish/requirements/gpg/ |
| Maven Source Plugin | https://maven.apache.org/plugins/maven-source-plugin/ |
| Maven Javadoc Plugin | https://maven.apache.org/plugins/maven-javadoc-plugin/ |
| Maven GPG Plugin | https://maven.apache.org/plugins/maven-gpg-plugin/ |
