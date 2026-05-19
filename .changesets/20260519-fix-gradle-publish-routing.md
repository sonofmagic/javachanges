---
"javachanges": patch
---

修复同时包含 `pom.xml` 和 Gradle 配置的仓库发布路由，`publish` 会自动走 Gradle 发布，并正确使用 `VERSION_NAME` 等 Gradle 版本属性。
