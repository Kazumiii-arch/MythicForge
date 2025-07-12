// ========================================================================
// File: build.gradle.kts
// Project: MythicForge
// ========================================================================
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// Set the group and version for the MythicForge plugin
group = "com.vortex"
version = "1.0.0"

// Define all repositories needed to download dependencies
repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // Spigot API
    maven { url = uri("https://jitpack.io/") } // Vault, Citizens
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") } // PlaceholderAPI
}

// External plugin dependencies
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // ✅ Shading Citizens into the final plugin JAR with relocation
    implementation("com.github.CitizensDev:Citizens2:2.0.33-SNAPSHOT")
}

// Java 17 language level (required for modern Minecraft versions)
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// ShadowJar configuration
tasks {
    shadowJar {
        archiveBaseName.set("MythicForge")
        archiveClassifier.set("") // No '-all' suffix
        archiveVersion.set(project.version.toString())

        // ✅ Relocate Citizens to avoid classpath conflicts
        relocate("net.citizensnpcs", "com.vortex.libs.citizens")
    }

    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
