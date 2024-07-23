plugins {
    id("java")
    id("java-library")
    alias(libs.plugins.shadow)
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

// store the version as a variable,
// as we use it several times
val fullVersion = "1.0.0"

// project settings
group = "me.kubbidev.blocktune"
version = "1.0-SNAPSHOT"

base {
    archivesName.set("blocktune")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    // include source in when publishing
    withSourcesJar()
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")

    // internal dependencies
    compileOnly("me.kubbidev.nexuspowered:nexuspowered:1.0-SNAPSHOT")
    compileOnly("me.kubbidev.spellcaster:spellcaster:1.0-SNAPSHOT")

    // optional dependencies
    compileOnly("me.clip:placeholderapi:2.11.6")
}

// building task operations
tasks.processResources {
    filesMatching("plugin.yml") {
        expand("pluginVersion" to fullVersion)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName = "BlockTune-${fullVersion}.jar"

    dependencies {
        include(dependency("me.kubbidev.blocktune:.*"))
    }

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

artifacts {
    archives(tasks.shadowJar)
}