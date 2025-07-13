// ========================================================================
// File: build.gradle.kts
// Location: /build.gradle.kts
// Project: MythicForge (Using Correct Local JAR Version)
// ========================================================================
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.vortex"
version = "1.0.0"

repositories {
    // This tells Gradle to look in the 'libs' folder for JAR files.
    flatDir {
        dirs("libs")
    }
    
    // We still need these for Spigot, Vault, and PlaceholderAPI
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // UPDATED: This now points to the exact JAR file you have in your 'libs' folder.
    compileOnly(files("libs/FancyNpcs-2.6.0.280.jar"))
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
