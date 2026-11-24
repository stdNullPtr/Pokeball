import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    id("io.freefair.lombok") version "9.8.3"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

// Compiling against 1.21.11 with Java 21 bytecode keeps one jar loadable on 1.21.11 and every 26.x server.
val apiVersion = "1.21.11"
val targetJava = 21

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$apiVersion-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJava)
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = targetJava
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    // Latest server. Minecraft 26.x needs Java 25. The JetBrains Runtime allows hotswapping
    // structural changes (new methods/fields) from an IntelliJ debug session.
    runServer {
        minecraftVersion("26.3")
        runDirectory = layout.projectDirectory.dir("run/26.3")
        javaLauncher = project.javaToolchains.launcherFor {
            vendor = JvmVendorSpec.JETBRAINS
            languageVersion = JavaLanguageVersion.of(25)
        }
        jvmArgs("-XX:+AllowEnhancedClassRedefinition")
    }

    // Oldest supported server, to catch API drift.
    register<RunServer>("runServerOldest") {
        group = "run paper"
        description = "Runs a Paper $apiVersion server with this plugin."
        minecraftVersion(apiVersion)
        runDirectory = layout.projectDirectory.dir("run/$apiVersion")
        pluginJars.from(jar.flatMap { it.archiveFile })
        javaLauncher = project.javaToolchains.launcherFor {
            vendor = JvmVendorSpec.JETBRAINS
            languageVersion = JavaLanguageVersion.of(targetJava)
        }
        jvmArgs("-XX:+AllowEnhancedClassRedefinition")
    }
}
