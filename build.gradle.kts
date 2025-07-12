plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.vortex"
version = "1.0.0"

repositories {
    mavenCentral()

    // Spigot API
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }

    // PlaceholderAPI repo
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }

    // JitPack (for Vault or others)
    maven { url = uri("https://jitpack.io/") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // ✅ Use FancyNPCs from local JAR
    implementation(files("libs/FancyNPCs.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    shadowJar {
        archiveBaseName.set("MythicForge")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())

        // ✅ Relocate FancyNPCs to avoid conflicts
        relocate("de.oliver.fancynpcs", "com.vortex.libs.fancynpcs")
    }

    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
