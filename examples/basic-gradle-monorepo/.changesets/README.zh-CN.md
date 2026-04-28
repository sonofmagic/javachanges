# Changesets

这个目录保存 Gradle 示例的待发布变更。

package key 使用 `settings.gradle.kts` 里的 Gradle project name：

```md
---
"core": minor
"api": minor
---

Add release notes generation workflow.
```

在 `javachanges` 源码仓库根目录运行：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-gradle-monorepo"
```
