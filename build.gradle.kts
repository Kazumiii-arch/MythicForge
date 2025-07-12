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

    // JitPack for Citizens, Vault, etc.
    maven { url = uri("https://jitpack.io/") }

    // PlaceholderAPI repo
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // ✅ Use Citizens via GitHub commit (from JitPack)
    implementation("com.github.CitizensDev:Citizens2:b3f3c74")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    shadowJar {
        archiveBaseName.set("MythicForge")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())

        // ✅ Relocate Citizens to avoid runtime conflicts with the server's version
        relocate("net.citizensnpcs", "com.vortex.libs.citizens")
    }

    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
