import com.theseednetwork.stitch.UploadSpec
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("io.papermc.paperweight.core") version "2.0.0-beta.19" apply false
    id("com.theseednetwork.stitch") version "0.2.0"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
        options.isFork = true
        options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
    }
    tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test>().configureEach {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }

    extensions.configure<PublishingExtension> {
        repositories {
            maven("https://artifactory.papermc.io/artifactory/snapshots/") {
                name = "paperSnapshots"
                credentials(PasswordCredentials::class)
            }
        }
    }
}

tasks.register("printMinecraftVersion") {
    val mcVersion = providers.gradleProperty("mcVersion")
    doLast {
        println(mcVersion.get().trim())
    }
}

tasks.register("printPaperVersion") {
    val paperVersion = provider { project.version }
    doLast {
        println(paperVersion.get())
    }
}

tasks.register<Delete>("cleanSingularity") {
    group = "singularity"

    delete("$projectDir/paper-server/stitch")
}

tasks.register<Copy>("buildSingularity") {
    group = "singularity"
    dependsOn("cleanSingularity", ":paper-server:createMojmapBundlerJar")

    from("paper-server/build/libs") {
        include("paper-bundler-${version}-mojmap.jar")
        rename("paper-bundler-${version}-mojmap.jar", "singularity-${version}.jar")
    }

    into("paper-server/stitch")

    outputs.upToDateWhen { false }
}

stitch {
    upload { spec ->
        spec.type = UploadSpec.Type.SINGULARITY
        spec.componentType = UploadSpec.ComponentType.BASE
        spec.localPath = "$projectDir/paper-server/stitch"
    }
}
