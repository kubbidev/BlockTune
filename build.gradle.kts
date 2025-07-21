import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import org.gradle.api.tasks.testing.logging.TestLogEvent

apply("gradle/ver.gradle.kts")
plugins {
    id("java")
    id("maven-publish")
    alias(libs.plugins.shadow)
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

group = "me.kubbidev"

base {
    archivesName.set("blocktune")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    repositories {
        maven(url = "https://nexus.kubbidev.me/repository/maven-releases/") {
            name = "kubbidev-releases"
            credentials(PasswordCredentials::class) {
                username = System.getenv("GRADLE_KUBBIDEV_RELEASES_USER") ?: property("kubbidev-releases-user") as String?
                password = System.getenv("GRADLE_KUBBIDEV_RELEASES_PASS") ?: property("kubbidev-releases-pass") as String?
            }
        }
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("me.kubbidev:spellcaster:2.0.0")
    compileOnly("me.kubbidev:nexuspowered:2.0.0")

    // Unit tests
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("pluginVersion" to "$version")
    }
}

tasks.shadowJar {
    archiveFileName = "BlockTune-$version.jar"
    mergeServiceFiles()
    dependencies {
        include(dependency("me.kubbidev:.*"))
    }

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.matching { it.name.startsWith("publish") }.configureEach {
    doFirst {
        if (version.toString().contains('+')) {
            throw GradleException("Refusing to publish non-release version: $version (tag a release first)")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}

artifacts {
    archives(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "blocktune"

            from(components["java"])
            pom {
                name = "BlockTune"
                description =
                    "An experimental Minecraft plugin used in my build servers project that helps me with a few extra features, allowing me to create and manage blocks with ease."
                url = "https://github.com/kubbidev/BlockTune"

                licenses {
                    license {
                        name = "CC BY-NC-SA 4.0"
                        url = "https://creativecommons.org/licenses/by-nc-sa/4.0/"
                    }
                }

                developers {
                    developer {
                        id = "kubbidev"
                        name = "kubbi"
                        url = "https://kubbidev.me"
                    }
                }

                issueManagement {
                    system = "Github"
                    url = "https://github.com/kubbidev/BlockTune/issues"
                }
            }
        }
    }
    repositories {
        maven(url = "https://nexus.kubbidev.me/repository/maven-releases/") {
            name = "kubbidev-releases"
            credentials(PasswordCredentials::class) {
                username = System.getenv("GRADLE_KUBBIDEV_RELEASES_USER") ?: property("kubbidev-releases-user") as String?
                password = System.getenv("GRADLE_KUBBIDEV_RELEASES_PASS") ?: property("kubbidev-releases-pass") as String?
            }
        }
    }
}