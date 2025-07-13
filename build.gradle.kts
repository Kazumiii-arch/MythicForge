// ========================================================================
// File: build.gradle.kts
// Location: /build.gradle.kts
// Project: MythicForge (Definitive Final Version)
// ========================================================================
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.vortex"
version = "1.0.0"

// Define all the repositories needed to download your plugin's dependencies.
repositories {
    // This tells Gradle to look in the 'libs' folder for JAR files.
    // It is a crucial part of the fix for the FancyNpcs dependency.
    flatDir {
        dirs("libs")
    }

    // These are still needed for the other dependencies.
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // For Spigot API
    maven { url = uri("https://jitpack.io/") } // For Vault API
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") } // For PlaceholderAPI
}

// Define all the external plugins MythicForge needs to hook into.
dependencies {
    // Standard Spigot API
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    // API Hooks
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // CORRECTED & FINAL: This robustly includes ALL .jar files from your 'libs' folder.
    // This is the definitive fix for the FancyNpcs dependency error.
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

// Set the project to use Java 17.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// Configure the output JAR file.
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
