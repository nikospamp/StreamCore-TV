// Top-level build file where you can add configuration options common to all subprojects/modules.

plugins {
    // Adds root lifecycle tasks such as `check` and `build`.
    // The root project does not compile code itself, but we need `check`
    // so the design-token verifier can be part of the normal verification flow.
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
}

// Typed task instead of `tasks.register { doLast { ... } }` so Gradle can
// understand the task inputs/outputs and keep configuration cache support.
abstract class VerifyDesignTokensTask : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    // Only Kotlin production source files are inputs. Do not use broad folders
    // like `core/` or Gradle will also see generated build outputs and report
    // false implicit-dependency problems.
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    // Token definition files are allowed to contain raw dp/sp/color values
    // because they are the source of truth for the design system.
    @get:Input
    abstract val allowedTokenFiles: ListProperty<String>

    @get:Input
    abstract val rawDimensionPattern: Property<String>

    @get:Input
    abstract val roundedCornerShapePattern: Property<String>

    @get:Input
    abstract val rawColorPattern: Property<String>

    @get:Input
    abstract val logCheckedFiles: Property<Boolean>

    // Optional on purpose:
    // - `verifyDesignTokens` sets this marker so Gradle can mark it UP-TO-DATE.
    // - `verifyDesignTokensLogFiles` leaves it unset so the debug task always
    //   executes and prints the checked file list from the Gradle UI.
    @get:Optional
    @get:OutputFile
    abstract val resultMarker: RegularFileProperty

    @TaskAction
    fun verify() {
        val rootDir = rootDirectory.get().asFile
        val rawDimensionRegex = Regex(rawDimensionPattern.get())
        val roundedCornerShapeRegex = Regex(roundedCornerShapePattern.get())
        val rawColorRegex = Regex(rawColorPattern.get())
        val allowedTokenFileSet = allowedTokenFiles.get().toSet()
        val checkedFiles = mutableListOf<String>()
        val violations = mutableListOf<String>()

        sourceFiles.files
            .filter { sourceFile -> sourceFile.isFile && sourceFile.extension == "kt" }
            .forEach { sourceFile ->
                val normalizedPath = sourceFile.relativeTo(rootDir).path.replace('\\', '/')
                if (normalizedPath in allowedTokenFileSet) {
                    return@forEach
                }

                checkedFiles += normalizedPath
                sourceFile.readLines().forEachIndexed { index, line ->
                    val match = roundedCornerShapeRegex.find(line)
                        ?: rawDimensionRegex.find(line)
                        ?: rawColorRegex.find(line)
                    if (match != null) {
                        violations += "$normalizedPath:${index + 1}: ${line.trim()}"
                    }
                }
            }

        if (logCheckedFiles.get()) {
            logger.lifecycle("$path checked ${checkedFiles.size} files:")
            checkedFiles.sorted().forEach { checkedFile ->
                logger.lifecycle(" - $checkedFile")
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Raw design values are not allowed in production UI code.")
                    appendLine("Use StreamCoreDimens, MaterialTheme.typography, MaterialTheme.shapes, and MaterialTheme.colorScheme instead.")
                    appendLine("Allowed raw values: 0.dp and token files Dimens.kt, Shape.kt, Type.kt, Color.kt, ColorSchemeExtensions.kt.")
                    appendLine()
                    violations.forEach { violation -> appendLine(violation) }
                },
            )
        }

        resultMarker.orNull?.asFile?.let { markerFile ->
            markerFile.parentFile.mkdirs()
            markerFile.writeText("OK\n")
        }
    }
}

val designTokenRootDir = layout.projectDirectory.asFile

// FileTree inputs keep the verifier scoped to source files only.
// This avoids depending on compile/resource task outputs while still letting
// Gradle track changes incrementally.
val designTokenSourceFiles = files(
    fileTree("core") {
        include("**/src/main/kotlin/**/*.kt")
    },
    fileTree("feature") {
        include("**/src/main/kotlin/**/*.kt")
    },
    fileTree("app") {
        include("**/src/main/kotlin/**/*.kt")
    },
)
val allowedDesignTokenFiles = setOf(
    "core/ui/src/main/kotlin/com/pampoukidis/streamcoretv/core/ui/extensions/ColorSchemeExtensions.kt",
    "core/ui/src/main/kotlin/com/pampoukidis/streamcoretv/core/ui/theme/Color.kt",
    "core/ui/src/main/kotlin/com/pampoukidis/streamcoretv/core/ui/theme/Dimens.kt",
    "core/ui/src/main/kotlin/com/pampoukidis/streamcoretv/core/ui/theme/Shape.kt",
    "core/ui/src/main/kotlin/com/pampoukidis/streamcoretv/core/ui/theme/Type.kt",
)
val rawDimensionRegexPatternValue = """(?<![A-Za-z0-9_])(?:[1-9]\d*(?:\.\d+)?|0\.\d+)\.(?:dp|sp)\b"""
val roundedCornerShapeRegexPatternValue = """RoundedCornerShape\s*\([^)]*(?:[1-9]\d*(?:\.\d+)?|0\.\d+)\.dp"""
val rawColorRegexPatternValue =
    """(?:Color\(\s*0x[0-9A-Fa-f_]+[uUL]*\s*\)|Color\.(?:White|Black|Red|Blue|Green|Yellow|Transparent|Gray|Grey|DarkGray|LightGray))"""

// CI/default verifier. This is wired into `check` and writes a tiny marker file
// after successful validation so repeated checks are incremental.
val verifyDesignTokens by tasks.registering(VerifyDesignTokensTask::class) {
    group = "verification"
    description = "Fails production Kotlin UI code that bypasses StreamCore design tokens."
    rootDirectory.set(layout.projectDirectory)
    sourceFiles.from(designTokenSourceFiles)
    allowedTokenFiles.set(allowedDesignTokenFiles.toList())
    rawDimensionPattern.set(rawDimensionRegexPatternValue)
    roundedCornerShapePattern.set(roundedCornerShapeRegexPatternValue)
    rawColorPattern.set(rawColorRegexPatternValue)
    logCheckedFiles.set(
        providers.gradleProperty("verifyDesignTokensLogFiles")
            .orElse(providers.gradleProperty("verifyDesignTokens.logFiles"))
            .map(String::toBoolean)
            .orElse(false),
    )
    resultMarker.set(layout.buildDirectory.file("reports/verifyDesignTokens/result.txt"))
}

// Manual diagnostic task for the Gradle UI. It intentionally has no output
// marker, so it always runs and logs every checked file.
val verifyDesignTokensLogFiles by tasks.registering(VerifyDesignTokensTask::class) {
    group = "verification"
    description = "Runs design-token verification and logs every checked Kotlin source file."
    rootDirectory.set(layout.projectDirectory)
    sourceFiles.from(designTokenSourceFiles)
    allowedTokenFiles.set(allowedDesignTokenFiles.toList())
    rawDimensionPattern.set(rawDimensionRegexPatternValue)
    roundedCornerShapePattern.set(roundedCornerShapeRegexPatternValue)
    rawColorPattern.set(rawColorRegexPatternValue)
    logCheckedFiles.set(true)
}

// Root `check` and every subproject `check` should enforce the design-system
// rule before code is considered verified.
tasks.named("check") {
    dependsOn(verifyDesignTokens)
}

subprojects {
    tasks.matching { task -> task.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("verifyDesignTokens"))
    }
}
