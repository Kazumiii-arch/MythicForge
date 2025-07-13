plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.vortex"
version = "1.0.0"

repositories {
    mavenCentral()

    // For Spigot API
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }

    // For Vault, FancyNpcs, and other GitHub-hosted libs
    maven { url = uri("https://jitpack.io/") }

    // For PlaceholderAPI
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
}

dependencies {
    // Spigot API
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")

    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.5")

    // ✅ FancyNpcs API from JitPack (adjust version as needed)
    compileOnly("com.github.OnlyNoobDied:FancyNpcs:1.20.1") // or latest release
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
