// ========================================================================
// File: build.gradle.kts
// Location: /build.gradle.kts
// Project: MythicForge (Adapted for FancyNPC)
// ========================================================================
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.vortex"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io/") } // For Vault and FancyNPCs
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // REMOVED: Citizens dependency
    // ADDED: FancyNpcs dependency, which is found on Jitpack
    compileOnly("de.oliver:FancyNpcs:2.5.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    shadowJar {
        archiveBaseName.set("MythicForge")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())
    }

    build {
        dependsOn(shadowJar)
    }
    
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
