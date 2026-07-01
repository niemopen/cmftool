import org.gradle.api.DefaultTask
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject
import java.time.LocalDate

plugins {
    base
}

abstract class RenderPandocDocs @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val appNames: ListProperty<String>

    @TaskAction
    fun render() {
        val sourceRoot = sourceDir.get().asFile
        val outRoot = outputDir.get().asFile

        if (outRoot.exists()) {
            outRoot.deleteRecursively()
        }
        outRoot.mkdirs()

        fun renderBucket(bucketName: String, bucketSourceDir: File) {
            if (!bucketSourceDir.isDirectory) return

            val bucketOutDir = outRoot.resolve(bucketName)
            bucketOutDir.mkdirs()

            bucketSourceDir
                .listFiles()
                ?.filter { it.isFile && it.extension == "md" }
                ?.sortedBy { it.name }
                .orEmpty()
                .forEach { md ->
                    val html = bucketOutDir.resolve("${md.nameWithoutExtension}.html")

                    execOperations.exec {
                        commandLine(
                            "pandoc",
                    //        "--standalone",
                            "--from", "gfm",
                            "--to", "html",
                            md.absolutePath,
                            "-o", html.absolutePath
                        )
                    }.assertNormalExitValue()
                }
        }

        renderBucket("shared", sourceRoot)

        appNames.get().forEach { appName ->
            renderBucket(appName, sourceRoot.resolve(appName))
        }
    }
}

data class AppSpec(
    val projectPath: String,
    val launcherName: String
)

val appSpecs = listOf(
    AppSpec(":app-cmftool", "cmftool"),
    AppSpec(":app-niemtran", "niemtran"),
    AppSpec(":app-scheval", "scheval")
)

val implementationTitleByProjectPath = mapOf(
    ":app-cmftool" to "CMFTool",
    ":app-niemtran" to "NIEMTran",
    ":app-scheval" to "SCHEval"
)

val launcherNameByProjectPath = appSpecs.associate { it.projectPath to it.launcherName }

fun String.capitalized(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }

        dependencies {
            add("testImplementation", platform("org.junit:junit-bom:5.12.2"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    pluginManager.withPlugin("java-library") {
        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }
    }

    pluginManager.withPlugin("application") {
        val implementationTitle = implementationTitleByProjectPath[path]
            ?: error("No Implementation-Title defined for project $path")

        tasks.named<Jar>("jar") {
            manifest {
                attributes(
                    "Implementation-Title" to implementationTitle,
                    "Implementation-Version" to project.version.toString(),
                    "Build-Date" to LocalDate.now().toString()
                )
            }
        }
    }
}

val docsSourceDir = layout.projectDirectory.dir("docs")
val renderedDocsRoot = layout.buildDirectory.dir("generated-docs/rendered")
val stagedDocsRoot = layout.buildDirectory.dir("generated-docs/staged")

val test by tasks.registering {
    group = "verification"
    description = "Runs tests for all subprojects"
}

subprojects {
    pluginManager.withPlugin("java") {
        val subprojectTest = tasks.named("test")

        rootProject.tasks.named("test") {
            dependsOn(subprojectTest)
        }
    }
}

val renderDocs by tasks.registering(RenderPandocDocs::class) {
    group = "documentation"
    description = "Render shared and app-specific markdown to HTML with pandoc"

    sourceDir.set(docsSourceDir)
    outputDir.set(renderedDocsRoot)
    appNames.set(appSpecs.map { it.launcherName })
}

val stageDocsTasks: Map<String, TaskProvider<Sync>> =
    appSpecs.associate { spec ->
        spec.launcherName to tasks.register<Sync>("stage${spec.launcherName.capitalized()}Docs") {
            group = "documentation"
            description = "Stage docs for ${spec.launcherName}"

            dependsOn(renderDocs)
            duplicatesStrategy = DuplicatesStrategy.FAIL

            into(stagedDocsRoot.map { it.dir(spec.launcherName) })

            from(renderDocs.flatMap { it.outputDir }.map { it.dir("shared") })
            from(renderDocs.flatMap { it.outputDir }.map { it.dir(spec.launcherName) })

            from(docsSourceDir.dir("examples")) {
                into("examples")
            }
        }
    }

val stageAllDocs by tasks.registering(Sync::class) {
    group = "documentation"
    description = "Stage shared docs, all app docs, and examples"

    dependsOn(renderDocs)
    duplicatesStrategy = DuplicatesStrategy.FAIL

    into(stagedDocsRoot.map { it.dir("allApps") })

    from(renderDocs.flatMap { it.outputDir }.map { it.dir("shared") })

    appSpecs.forEach { spec ->
        from(renderDocs.flatMap { it.outputDir }.map { it.dir(spec.launcherName) })
    }

    from(docsSourceDir.dir("examples")) {
        into("examples")
    }
}

subprojects {
    pluginManager.withPlugin("application") {
        val launcherName = launcherNameByProjectPath[path]
            ?: error("No launcher mapping defined for project $path")

        val stageDocsTask = stageDocsTasks.getValue(launcherName)

        tasks.named<Sync>("installDist") {
            dependsOn(stageDocsTask)
        }

        tasks.named("distZip") {
            enabled = false
        }

        tasks.named("distTar") {
            enabled = false
        }

        extensions.configure<DistributionContainer> {
            named("main") {
                contents {
                    from(stageDocsTask) {
                        into("docs")
                    }
            
                    from(rootProject.layout.projectDirectory.file("README.md"))

                    from(layout.projectDirectory.file("README.md")) {
                        rename { "README-$launcherName.md" }
                    }
                }
            }
        }
    }
}

val allAppsImage by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Assemble merged install image for all applications at build/install/allApps"

    dependsOn(stageAllDocs)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    into(layout.buildDirectory.dir("install/allApps"))

    from(stageAllDocs) {
        into("docs")
    }
}

val releaseZips by tasks.registering {
    group = "distribution"
    description = "Build all release zip files"
}

appSpecs.forEach { spec ->
    val appProject = project(spec.projectPath)

    appProject.pluginManager.withPlugin("application") {
        val appInstallDist = appProject.tasks.named<Sync>("installDist")

        allAppsImage.configure {
            dependsOn(appInstallDist)

            from(appInstallDist) {
                exclude("docs/**")
            }
        }

        val releaseZip = rootProject.tasks.register<Zip>("${spec.launcherName}ReleaseZip") {
            group = "distribution"
            description = "Build ${spec.launcherName}-${rootProject.version}.zip"

            dependsOn(appInstallDist)

            archiveFileName.set("${spec.launcherName}-${rootProject.version}.zip")
            destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))

            from(appInstallDist)
        }

        releaseZips.configure {
            dependsOn(releaseZip)
        }
    }
}

val allAppsZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Build cmftool-allApps-${project.version}.zip"

    dependsOn(allAppsImage)

    archiveFileName.set("cmftool-allApps-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(allAppsImage)
}

releaseZips.configure {
    dependsOn(allAppsZip)
}
