import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    base
}

subprojects {
    group = "io.github.sonofmagic.examples"
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension>("java") {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    repositories {
        mavenCentral()
    }

    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
        repositories {
            maven {
                name = "example"
                url = uri(layout.buildDirectory.dir("repo"))
            }
        }
    }
}
