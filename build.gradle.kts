import io.papermc.paperweight.util.*
import io.papermc.paperweight.util.constants.*

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.patcher") version "1.5.15" // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
}

allprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    mavenCentral()
    maven(paperMavenPublicUrl) {
        content {
            onlyForConfigurations(PAPERCLIP_CONFIG)
        }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.10.1:fat")
    decompiler("net.minecraftforge:forgeflower:2.0.627.2")
    paperclip("io.papermc:paperclip:3.0.3")
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }
    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
        maven("https://jitpack.io")
    }
}

paperweight {
    serverProject.set(project(":bloom-server"))

    remapRepo.set(paperMavenPublicUrl)
    decompileRepo.set(paperMavenPublicUrl)

    useStandardUpstream("Folia") {
        url.set(github("PaperMC", "Folia"))
        ref.set(providers.gradleProperty("foliaRef"))
        
        withStandardPatcher {
            baseName("Folia")

            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
            apiOutputDir.set(layout.projectDirectory.dir("bloom-api"))

            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
            serverOutputDir.set(layout.projectDirectory.dir("bloom-server"))
        }
        patchTasks.register("generatedApi") {
            isBareDirectory = true
            upstreamDirPath = "paper-api-generator/generated"
            patchDir = layout.projectDirectory.dir("patches/generatedApi")
            outputDir = layout.projectDirectory.dir("paper-api-generator/generated")
        }
    }
}

tasks.register("foliaRefLatest") {
    // Update the foliaRef in gradle.properties to be the latest commit.
    val tempDir = layout.cacheDir("foliaRefLatest");
    val file = "gradle.properties";
        
    doFirst {
        data class GithubCommit(
                val sha: String
        )

        val foliaLatestCommitJson = layout.cache.resolve("foliaLatestCommit.json");
        download.get().download("https://api.github.com/repos/PaperMC/Folia/commits/master", foliaLatestCommitJson);
        val foliaLatestCommit = gson.fromJson<paper.libs.com.google.gson.JsonObject>(foliaLatestCommitJson)["sha"].asString;

        copy {
            from(file)
            into(tempDir)
            filter { line: String ->
                line.replace("foliaRef = .*".toRegex(), "foliaRef = $foliaLatestCommit")
            }
        }
    }

    doLast {
        copy {
            from(tempDir.file("gradle.properties"))
            into(project.file(file).parent)
        }
    }
}
