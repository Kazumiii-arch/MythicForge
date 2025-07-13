// ========================================================================
// File: build.gradle.kts
// Location: /build.gradle.kts
// Project: MythicForge (Definitive Final Version)
// ========================================================================

// Using the java-library and maven-publish plugins for a professional setup
plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.6"
}

// Set the group and version for the MythicForge plugin
group = "com.vortex"
version = "1.0.0"

// Define the Minecraft version for dependency management
val minecraftVersion = "1.20.1"

// Define all the repositories needed to download our plugin's dependencies
repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // For Paper API
    maven { url = uri("https://jitpack.io/") } // For Vault API
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") } // For PlaceholderAPI
    
    // ADDED: The official repository for FancyNpcs and its required libraries
    maven { url = uri("https://repo.fancyplugins.de/releases") }
}

// Define all the external plugins MythicForge needs to hook into
dependencies {
    // Use Paper API for better performance and more features
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion-R0.1-SNAPSHOT")

    // API Hooks
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // CORRECTED: This now correctly declares FancyNpcs and its required library, FancyLib.
    // This will resolve all the "cannot find symbol" errors related to the NPC system.
    compileOnly("de.oliver:FancyNpcs:2.6.0")
    compileOnly("de.oliver:FancyLib:37")
}

// Configure the output JAR file using the shadow plugin
tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("MythicForge")
        archiveVersion.set(project.version.toString())
        
        // This is not strictly necessary unless we add a library that needs relocation,
        // but it's good practice to have the configuration ready.
        // relocate("package.to.relocate", "com.vortex.mythicforge.libs.relocatedpackage")
    }

    // This ensures that running 'build' will also create our final JAR.
    build {
        dependsOn(shadowJar)
    }

    // Configure the Java compilation settings
    java {
        withSourcesJar()
        withJavadocJar()
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
}
