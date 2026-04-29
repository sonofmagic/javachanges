rootProject.name = "basic-gradle-monorepo"

include(":core", ":api")

project(":core").projectDir = file("modules/core")
project(":api").projectDir = file("modules/api")
