// ========================================================================
// File: build.gradle.kts
// Location: /build.gradle.kts
// Project: MythicForge
// ========================================================================
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// Set the group and version for the MythicForge plugin
group = "com.vortex"
version = "1.0.0"

// Define all the repositories needed to download our plugin's dependencies
repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // For Spigot API
    maven { url = uri("https://jitpack.io/") } // For Vault and Citizens
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") } // For PlaceholderAPI
}

// Define all the external plugins MythicForge needs to hook into
dependencies {
    // These are marked 'compileOnly' because the server will provide them at runtime
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    
    // The full Citizens API artifact
    compileOnly("com.github.CitizensDev.Citizens2:citizens-main:2.0.33-SNAPSHOT")
}

// Set the project to use Java 17, as required for modern Minecraft versions
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// Configure the output JAR file
tasks {
    shadowJar {
        archiveBaseName.set("MythicForge") // Set the final JAR name
        archiveClassifier.set("") // Remove the '-all' suffix
        archiveVersion.set(project.version.toString())
    }

    build {
        dependsOn(shadowJar)
    }
    
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
