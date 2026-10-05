import org.gradle.api.DefaultTask
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.application.tasks.CreateStartScripts
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.process.ExecOperations
import groovy.json.JsonSlurper
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

plugins {
    base
    id("org.cyclonedx.bom") version "3.4.1" apply false
}

group = providers.gradleProperty("projectGroup").get()
version = providers.gradleProperty("bundleVersion").get()

val bundleName = providers.gradleProperty("bundleName").get()
val bundleDisplayName = rootProject.name
val javaVersion = providers.gradleProperty("javaVersion").get().toInt()
val cmfVersion = providers.gradleProperty("cmfVersion").get()
val utilVersion = providers.gradleProperty("utilVersion").get()
val schevalVersion = providers.gradleProperty("schevalVersion").get()

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
                            "--standalone",
                            "--from", "gfm",
                            "--to", "html",
                            "--css", "styles.css",
                            "-V", "maxwidth=100%",
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

abstract class GenerateBundleReadme : DefaultTask() {

    @get:Input
    abstract val bundleDisplayName: Property<String>

    @get:Input
    abstract val bundleVersionText: Property<String>

    @get:Input
    abstract val cmftoolVersionText: Property<String>

    @get:Input
    abstract val niemtranVersionText: Property<String>

    @get:Input
    abstract val schevalVersionText: Property<String>

    @get:Input
    abstract val libCmfVersionText: Property<String>

    @get:Input
    abstract val libUtilVersionText: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()

        file.writeText(
            """
            # ${bundleDisplayName.get()} bundle contents

            This release bundle includes:

            - CMFTool application: ${cmftoolVersionText.get()}
            - NIEMTran application: ${niemtranVersionText.get()}
            - SCHEval application: ${schevalVersionText.get()}

            Included libraries:

            - lib-cmf: ${libCmfVersionText.get()}
            - lib-util: ${libUtilVersionText.get()}

            Bundle version:

            - ${bundleDisplayName.get()}: ${bundleVersionText.get()}
            """.trimIndent() + "\n"
        )
    }
}


abstract class GenerateBundleSbom @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val source = sourceDir.get().asFile
        val outFile = outputFile.get().asFile

        outFile.parentFile.mkdirs()

        execOperations.exec {
            commandLine(
                "syft",
                "dir:${source.absolutePath}",
                "-o",
                "cyclonedx-json=${outFile.absolutePath}"
            )
        }.assertNormalExitValue()
    }
}

abstract class GenerateSbomMarkdownReports : DefaultTask() {

    @get:InputDirectory
    abstract val inputRoot: DirectoryProperty

    @get:OutputDirectory
    abstract val outputRoot: DirectoryProperty

    @TaskAction
    fun generate() {
        val inRoot = inputRoot.get().asFile
        val outRoot = outputRoot.get().asFile

        if (!inRoot.exists()) {
            logger.lifecycle("No SBOM directory found at ${inRoot.absolutePath}")
            return
        }

        if (outRoot.exists()) {
            outRoot.deleteRecursively()
        }
        outRoot.mkdirs()

        val jsonFiles = inRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .sortedBy { it.relativeTo(inRoot).path }
            .toList()

        if (jsonFiles.isEmpty()) {
            logger.lifecycle("No SBOM JSON files found under ${inRoot.absolutePath}")
            return
        }

        val slurper = JsonSlurper()

        fun mapValue(value: Any?): Map<*, *> = value as? Map<*, *> ?: emptyMap<Any, Any>()
        fun listValue(value: Any?): List<*> = value as? List<*> ?: emptyList<Any>()
        fun str(value: Any?): String = value?.toString() ?: ""

        fun licenseString(component: Map<*, *>): String {
            val licenses = listValue(component["licenses"])
            val values = licenses.mapNotNull { entry ->
                val entryMap = mapValue(entry)
                val license = mapValue(entryMap["license"])
                when {
                    license["id"] != null -> str(license["id"])
                    license["name"] != null -> str(license["name"])
                    entryMap["expression"] != null -> str(entryMap["expression"])
                    else -> null
                }
            }
            return values.joinToString(", ")
        }

        fun esc(text: String): String =
            text.replace("|", "\\|").replace("\n", " ").trim()

        jsonFiles.forEach { jsonFile ->
            val relativePath = jsonFile.relativeTo(inRoot)
            val outFile = outRoot.resolve(
                relativePath.path.removeSuffix(".json") + ".md"
            )
            outFile.parentFile.mkdirs()

            @Suppress("UNCHECKED_CAST")
            val data = slurper.parse(jsonFile) as Map<String, Any?>

            val metadata = mapValue(data["metadata"])
            val subject = mapValue(metadata["component"])
            val components = listValue(data["components"])
                .map { mapValue(it) }
                .sortedWith(
                    compareBy(
                        { str(it["type"]) },
                        { str(it["group"]) },
                        { str(it["name"]) },
                        { str(it["version"]) }
                    )
                )

            val lines = mutableListOf<String>()
            lines += "# SBOM Report: ${jsonFile.name}"
            lines += ""
            lines += "- Source: `${relativePath.path.replace(File.separatorChar, '/')}`"
            lines += "- Spec version: ${str(data["specVersion"])}"
            lines += "- Serial number: ${str(data["serialNumber"])}"
            lines += "- SBOM version: ${str(data["version"])}"
            lines += "- Component count: ${components.size}"
            lines += ""

            if (subject.isNotEmpty()) {
                lines += "## Subject"
                lines += ""
                lines += "- Type: ${str(subject["type"])}"
                lines += "- Group: ${str(subject["group"])}"
                lines += "- Name: ${str(subject["name"])}"
                lines += "- Version: ${str(subject["version"])}"
                val purl = str(subject["purl"])
                if (purl.isNotEmpty()) {
                    lines += "- PURL: `$purl`"
                }
                lines += ""
            }

            lines += "## Components"
            lines += ""
            lines += "| Type | Group | Name | Version | License | PURL |"
            lines += "|---|---|---|---|---|---|"

            components.forEach { c ->
                val type = esc(str(c["type"]))
                val group = esc(str(c["group"]))
                val name = esc(str(c["name"]))
                val version = esc(str(c["version"]))
                val license = esc(licenseString(c))
                val purl = str(c["purl"]).let { if (it.isBlank()) "" else "`$it`" }

                lines += "| $type | $group | $name | $version | $license | $purl |"
            }

            lines += ""
            outFile.writeText(lines.joinToString("\n"))
            logger.lifecycle("Wrote ${outFile.absolutePath}")
        }
    }
}

abstract class VerifyReleaseVersions : DefaultTask() {

    @get:Input
    abstract val versionsByPath: MapProperty<String, String>

    @TaskAction
    fun verify() {
        val offenders = versionsByPath.get()
            .filterValues { it.endsWith("-SNAPSHOT", ignoreCase = true) }
            .map { (path, version) -> "$path -> $version" }

        if (offenders.isNotEmpty()) {
            error(
                "Release build cannot use SNAPSHOT versions:\n" +
                    offenders.joinToString("\n")
            )
        }
    }
}

data class AppSpec(
    val projectPath: String,
    val launcherName: String,
    val implementationTitle: String,
    val mainClassName: String
)

val appSpecs = listOf(
    AppSpec(":app-cmftool", "cmftool", "CMFTool", "org.mitre.niem.cmftool.CMFTool"),
    AppSpec(":app-niemtran", "niemtran", "NIEMTran", "org.mitre.niem.translate.NIEMTran"),
    AppSpec(":app-scheval", "scheval", "SCHEval", "org.mitre.niem.scheval.SCHEval")
)

val appSpecByPath = appSpecs.associateBy { it.projectPath }

val projectVersions = mapOf(
    ":lib-cmf" to cmfVersion,
    ":app-cmftool" to cmfVersion,
    ":app-niemtran" to cmfVersion,
    ":lib-util" to utilVersion,
    ":app-scheval" to schevalVersion
)

val sbomProjectPaths = listOf(
    ":lib-util",
    ":lib-cmf",
    ":app-cmftool",
    ":app-niemtran",
    ":app-scheval"
)

fun String.capitalized(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

fun String.isSnapshotVersion(): Boolean =
    endsWith("-SNAPSHOT", ignoreCase = true)

subprojects {
    group = rootProject.group
    version = projectVersions[path] ?: error("No version defined for project $path")

    pluginManager.withPlugin("java") {
        pluginManager.apply("org.cyclonedx.bom")

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(javaVersion))
            }
        }

        dependencies {
            add("testImplementation", platform(libs.junit.bom))
            add("testImplementation", libs.junit.jupiter)
            add("testRuntimeOnly", libs.junit.platform.launcher)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    pluginManager.withPlugin("java-library") {
        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
        }
    }

    pluginManager.withPlugin("application") {
        val spec = appSpecByPath[path] ?: error("No app metadata defined for project $path")

        extensions.configure<JavaApplication> {
            mainClass.set(spec.mainClassName)
        }

        tasks.named<CreateStartScripts>("startScripts") {
            applicationName = spec.launcherName
        }

        tasks.named<Jar>("jar") {
            manifest {
                attributes(
                    "Implementation-Title" to spec.implementationTitle,
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
        rootProject.tasks.named("test") {
            dependsOn(tasks.named("test"))
        }
    }
}

val verifyReleaseVersions by tasks.registering(VerifyReleaseVersions::class) {
    group = "verification"
    description = "Fails if the release bundle would use a SNAPSHOT version"

    versionsByPath.put(":", rootProject.version.toString())
    versionsByPath.put(":lib-util", project(":lib-util").version.toString())
    versionsByPath.put(":lib-cmf", project(":lib-cmf").version.toString())
    versionsByPath.put(":app-cmftool", project(":app-cmftool").version.toString())
    versionsByPath.put(":app-niemtran", project(":app-niemtran").version.toString())
    versionsByPath.put(":app-scheval", project(":app-scheval").version.toString())
}


val renderDocs by tasks.registering(RenderPandocDocs::class) {
    group = "documentation"
    description = "Render shared and app-specific markdown to HTML with pandoc"

    sourceDir.set(docsSourceDir)
    outputDir.set(renderedDocsRoot)
    appNames.set(appSpecs.map { it.launcherName })
}

val stageDocsTasks =
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

    into(stagedDocsRoot.map { it.dir("all-apps") })

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
        val spec = appSpecByPath[path] ?: error("No launcher mapping defined for project $path")
        val stageDocsTask = stageDocsTasks.getValue(spec.launcherName)

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

                    from(project.layout.projectDirectory.file("README.md")) {
                        rename { "README-${spec.launcherName}.md" }
                    }
                }
            }
        }
    }
}

val generateBundleReadme by tasks.registering(GenerateBundleReadme::class) {
    group = "distribution"
    description = "Generate a bundle readme listing included component versions"

    bundleDisplayName.set(rootProject.name)
    bundleVersionText.set(rootProject.version.toString())
    cmftoolVersionText.set(project(":app-cmftool").version.toString())
    niemtranVersionText.set(project(":app-niemtran").version.toString())
    schevalVersionText.set(project(":app-scheval").version.toString())
    libCmfVersionText.set(project(":lib-cmf").version.toString())
    libUtilVersionText.set(project(":lib-util").version.toString())

    outputFile.set(layout.buildDirectory.file("generated-release-metadata/README-BUNDLE.md"))
}


val allAppsImage by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Assemble merged install image for all applications at build/install/$bundleName"

    dependsOn(stageAllDocs)
    dependsOn(generateBundleReadme)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    into(layout.buildDirectory.dir("install/$bundleName"))

    from(stageAllDocs) {
        into("docs")
    }

    from(rootProject.layout.projectDirectory.file("README.md"))
    from(generateBundleReadme)
}

appSpecs.forEach { spec ->
    val appProject = project(spec.projectPath)

    appProject.pluginManager.withPlugin("application") {
        val appInstallDist = appProject.tasks.named<Sync>("installDist")

        allAppsImage.configure {
            dependsOn(appInstallDist)

            from(appInstallDist) {
                exclude("docs/**")
                exclude("README.md")
                exclude("README-*.md")
            }
        }
    }
}

val sbomSubprojects by tasks.registering {
    group = "reporting"
    description = "Generate CycloneDX SBOMs for all Java subprojects"

    dependsOn(sbomProjectPaths.map { "$it:cyclonedxBom" })
}

val collectSubprojectSboms by tasks.registering(Sync::class) {
    group = "reporting"
    description = "Collect subproject SBOMs into one directory"

    dependsOn(sbomSubprojects)

    into(layout.buildDirectory.dir("reports/sbom/projects"))

    sbomProjectPaths.forEach { projectPath ->
        val p = project(projectPath)

        from(p.layout.buildDirectory.dir("reports")) {
            include("bom.json")
            include("bom.xml")
            into(p.name)
        }
    }
}

val bundleSbom by tasks.registering(GenerateBundleSbom::class) {
    group = "reporting"
    description = "Generate a CycloneDX SBOM for the assembled $bundleName bundle"

    dependsOn(allAppsImage)

    sourceDir.set(layout.buildDirectory.dir("install/$bundleName"))
    outputFile.set(layout.buildDirectory.file("reports/sbom/$bundleName-bundle.cdx.json"))
}

val sbom by tasks.registering {
    group = "reporting"
    description = "Generate subproject SBOMs and a bundle SBOM"

    dependsOn(collectSubprojectSboms)
    dependsOn(bundleSbom)
}

val sbomMarkdown by tasks.registering(GenerateSbomMarkdownReports::class) {
    group = "reporting"
    description = "Generate Markdown reports from all generated SBOM JSON files"

    dependsOn(sbom)

    inputRoot.set(layout.buildDirectory.dir("reports/sbom"))
    outputRoot.set(layout.buildDirectory.dir("reports/sbom-markdown"))
}

val releaseZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Build $bundleName-${project.version}.zip"

    dependsOn(verifyReleaseVersions)
    dependsOn(allAppsImage)

    archiveFileName.set("$bundleName-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(allAppsImage) {
        into("$bundleName-${project.version}")
    }
}
