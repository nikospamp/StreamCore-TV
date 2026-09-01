import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all subprojects/modules.

plugins {
    alias(libs.plugins.androidx.baselineprofile) apply false
    // Adds root lifecycle tasks such as `check` and `build`.
    // The root project does not compile code itself, but we need `check`
    // so the design-token verifier can be part of the normal verification flow.
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
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

        if (checkedFiles.isEmpty()) {
            throw GradleException(
                "Design-token verification checked zero production Kotlin files. " +
                        "Verify the configured main/commonMain/androidMain/wasmJsMain source roots.",
            )
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

abstract class VerifyKmpTestTargetsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val commonTestSourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val commonTestModules: ListProperty<String>

    @get:Input
    abstract val hostTestModules: ListProperty<String>

    @get:Input
    abstract val compileOnlyModules: ListProperty<String>

    @TaskAction
    fun verify() {
        val commonModules = commonTestModules.get().toSet()
        val hostModules = hostTestModules.get().toSet()
        val compileOnly = compileOnlyModules.get().toSet()
        val missingHostTests = commonModules - hostModules
        val invalidCompileOnlyTests = commonModules intersect compileOnly
        val invalidCompileOnlyTargets = hostModules intersect compileOnly

        if (missingHostTests.isNotEmpty() || invalidCompileOnlyTests.isNotEmpty() || invalidCompileOnlyTargets.isNotEmpty()) {
            throw GradleException(
                buildString {
                    if (missingHostTests.isNotEmpty()) {
                        appendLine("KMP modules with commonTest Kotlin files but no Android host-test compilation:")
                        missingHostTests.sorted().forEach { module -> appendLine(" - $module") }
                    }
                    if (invalidCompileOnlyTests.isNotEmpty()) {
                        appendLine("Compile-only KMP modules must not contain commonTest Kotlin files:")
                        invalidCompileOnlyTests.sorted().forEach { module -> appendLine(" - $module") }
                    }
                    if (invalidCompileOnlyTargets.isNotEmpty()) {
                        appendLine("Compile-only KMP modules must not enable Android host tests:")
                        invalidCompileOnlyTargets.sorted().forEach { module -> appendLine(" - $module") }
                    }
                },
            )
        }

        logger.lifecycle(
            "$path verified ${commonModules.size} KMP common-test module(s); " +
                    "${hostModules.size} Android host-test target(s); " +
                    "compile-only exemptions: ${compileOnly.sorted().joinToString()}",
        )
    }
}

abstract class VerifyKmpAndroidCompilerFlagsTask : DefaultTask() {
    @get:Input
    abstract val applicableCompileTasks: ListProperty<String>

    @get:Input
    abstract val missingFlagCompileTasks: ListProperty<String>

    @TaskAction
    fun verify() {
        val applicable = applicableCompileTasks.get()
        val missing = missingFlagCompileTasks.get()
        if (missing.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Compose KMP Android compilations missing -Xlambdas=class:")
                    missing.sorted().forEach { taskPath -> appendLine(" - $taskPath") }
                },
            )
        }

        logger.lifecycle(
            if (applicable.isEmpty()) {
                "$path found zero Compose KMP Android compile tasks; first nonzero proof is owned by KMP-06."
            } else {
                "$path verified -Xlambdas=class for ${applicable.size} Compose KMP Android compile task(s)."
            },
        )
    }
}

abstract class VerifyKmpDependencyCompatibilityTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val commonArtifacts: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val androidArtifacts: ConfigurableFileCollection

    @get:Input
    abstract val commonSelections: ListProperty<String>

    @get:Input
    abstract val androidSelections: ListProperty<String>

    @get:Input
    abstract val lockedCoordinates: ListProperty<String>

    @TaskAction
    fun verify() {
        val expectedCount = lockedCoordinates.get().size
        val common = commonArtifacts.files.filter(File::isFile)
        val android = androidArtifacts.files.filter(File::isFile)
        val commonVariants = commonSelections.get()
        val androidVariants = androidSelections.get()
        if (commonVariants.size != expectedCount || androidVariants.size != expectedCount) {
            throw GradleException(
                "Locked KMP compatibility resolution expected $expectedCount direct components per variant, " +
                        "but resolved common=${commonVariants.size}, android=${androidVariants.size}.",
            )
        }
        logger.lifecycle(
            "$path resolved $expectedCount locked KMP coordinates for common metadata and Android " +
                    "(${common.size}/${android.size} artifact files).",
        )
        commonVariants.forEach { selection -> logger.lifecycle(" - common: $selection") }
        androidVariants.forEach { selection -> logger.lifecycle(" - android: $selection") }
    }
}

abstract class VerifyKmpConventionPluginsTask : DefaultTask() {
    @get:Input
    abstract val fixtureModules: ListProperty<String>

    @get:Input
    abstract val missingCompileTasks: ListProperty<String>

    @get:Input
    abstract val missingConsumableVariants: ListProperty<String>

    @get:Input
    abstract val composeFixturesMissingAndroidFlag: ListProperty<String>

    @TaskAction
    fun verify() {
        val missingTasks = missingCompileTasks.get()
        val missingVariants = missingConsumableVariants.get()
        val missingFlags = composeFixturesMissingAndroidFlag.get()
        if (missingTasks.isNotEmpty() || missingVariants.isNotEmpty() || missingFlags.isNotEmpty()) {
            throw GradleException(
                buildString {
                    missingTasks.forEach { value -> appendLine("Missing convention fixture compile task: $value") }
                    missingVariants.forEach { value -> appendLine("Missing consumable Android variant: $value") }
                    missingFlags.forEach { value -> appendLine("Compose fixture missing -Xlambdas=class: $value") }
                },
            )
        }

        logger.lifecycle(
            "$path verified ${fixtureModules.get().size} convention-only fixture modules with " +
                    "registered Android targets, compileAndroidMain tasks, consumable variants, and Compose Android flags.",
        )
    }
}

val designTokenRootDir = layout.projectDirectory.asFile

// Root each FileTree at an explicit production source set. The task therefore
// receives individual Kotlin files without overlapping generated build outputs,
// while Gradle still detects added/removed files incrementally.
val designTokenSourceSetPaths = listOf(
    "src/main/kotlin",
    "src/main/java",
    "src/commonMain/kotlin",
    "src/androidMain/kotlin",
    "src/wasmJsMain/kotlin",
)
val designTokenProjects = subprojects.filter { subproject ->
    subproject.path == ":app" ||
            subproject.path.startsWith(":core:") ||
            subproject.path.startsWith(":feature:")
}
val designTokenSourceFiles = files(
    designTokenProjects.flatMap { subproject ->
        designTokenSourceSetPaths.map { sourceSetPath ->
            subproject.fileTree(sourceSetPath) {
                include("**/*.kt")
                exclude("**/build/**", "**/generated/**", "**/test/**", "**/androidTest/**")
            }
        }
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

val kmpCommonTestSourceFiles = files(
    subprojects.map { subproject ->
        subproject.fileTree("src/commonTest/kotlin") {
            include("**/*.kt")
        }
    },
)
val verifyKmpTestTargets by tasks.registering(VerifyKmpTestTargetsTask::class) {
    group = "verification"
    description = "Fails when common KMP tests have no executable Android host-test target."
    commonTestSourceFiles.from(kmpCommonTestSourceFiles)
    compileOnlyModules.set(listOf(":core:domain"))
}
val verifyKmpAndroidCompilerFlags by tasks.registering(VerifyKmpAndroidCompilerFlagsTask::class) {
    group = "verification"
    description = "Checks Compose KMP Android compilations for the live-edit lambda compiler mode."
}
val lockedKmpCompatibilityCoordinateNames = listOf(
    "androidx.datastore:datastore-core-okio:1.2.1",
    "io.coil-kt.coil3:coil-network-ktor3:3.4.0",
    "org.jetbrains.compose.components:components-resources:1.12.0",
    "org.jetbrains.compose.foundation:foundation:1.12.0",
    "org.jetbrains.compose.runtime:runtime:1.12.0",
    "org.jetbrains.compose.ui:ui:1.12.0",
    "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.10.0",
    "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0",
    "io.insert-koin:koin-compose:4.2.2",
    "io.insert-koin:koin-compose-viewmodel:4.2.2",
    "io.ktor:ktor-client-core:3.5.0",
    "io.ktor:ktor-client-content-negotiation:3.5.0",
    "io.ktor:ktor-serialization-kotlinx-json:3.5.0",
    "org.jetbrains.kotlinx:kotlinx-datetime:0.8.0",
)
val verifyKmpDependencyCompatibility by tasks.registering(VerifyKmpDependencyCompatibilityTask::class) {
    group = "verification"
    description = "Resolves the locked common and Android KMP dependency matrix without adding a web target."
    lockedCoordinates.set(lockedKmpCompatibilityCoordinateNames)
}
val verifyKmpConventionPlugins by tasks.registering(VerifyKmpConventionPluginsTask::class) {
    group = "verification"
    description = "Proves each KMP convention registers a usable Android target without module-local target creation."
}
val testAndroidHostTest by tasks.registering {
    group = "verification"
    description = "Runs every enabled KMP Android host-test suite."
}

// Root `check` and every subproject `check` should enforce the design-system
// rule before code is considered verified.
tasks.named("check") {
    dependsOn(
        verifyDesignTokens,
        verifyKmpTestTargets,
        verifyKmpAndroidCompilerFlags,
        verifyKmpDependencyCompatibility,
        verifyKmpConventionPlugins,
    )
}

subprojects {
    // Only Android-only libraries receive the app's benchmark/profile build types.
    // Official Android-KMP libraries stay single-variant and resolve from every app build type.
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension> {
            buildTypes {
                create("benchmark") {
                    initWith(getByName("release"))
                    matchingFallbacks += "release"
                }
                create("benchmarkR8") {
                    initWith(getByName("benchmark"))
                    matchingFallbacks += listOf("benchmark", "release")
                }
                create("profile") {
                    initWith(getByName("benchmark"))
                    matchingFallbacks += listOf("benchmark", "release")
                }
                create("nonMinifiedProfile") {
                    initWith(getByName("profile"))
                    matchingFallbacks += listOf("profile", "benchmark", "release")
                }
            }
        }
    }
    val traceModules = setOf(
        ":app",
        ":feature:home:ui-common", ":feature:home:ui-mobile",
        ":feature:details:ui-mobile", ":feature:details:ui-tablet",
        ":feature:search:ui-mobile",
        ":feature:player:ui-mobile",
    )
    if (path in traceModules) {
        listOf("com.android.application", "com.android.library").forEach { androidPlugin ->
            pluginManager.withPlugin(androidPlugin) {
                dependencies.add("implementation", project(":core:tracing"))
            }
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
        if (providers.gradleProperty("composeCompilerReports").orNull == "true") {
            tasks.withType<KotlinCompile>().configureEach {
                val isSingleVariantKmpAndroidMain =
                    pluginManager.hasPlugin("com.android.kotlin.multiplatform.library") && name == "compileAndroidMain"
                if (name.contains("Release") || name.contains("Benchmark") || isSingleVariantKmpAndroidMain) {
                    // Incremental compiler reports can describe only the changed files.
                    // This explicit diagnostic mode must report the whole module.
                    incremental = false
                    outputs.upToDateWhen { false }
                    outputs.cacheIf { false }
                    val reportDirectory = layout.buildDirectory.dir("reports/compose/$name")
                    compilerOptions.freeCompilerArgs.addAll(
                        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${reportDirectory.get().asFile}",
                        "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${reportDirectory.get().asFile}",
                    )
                }
            }
        }
    }
    val isLiveEditableUiModule =
        path == ":app" ||
                path == ":core:ui" ||
                ":ui-" in path

    if (isLiveEditableUiModule) {
        listOf(
            "com.android.application",
            "com.android.library",
        ).forEach { pluginId ->
            pluginManager.withPlugin(pluginId) {
                extensions.configure<KotlinAndroidProjectExtension> {
                    compilerOptions {
                        freeCompilerArgs.add("-Xlambdas=class")
                    }
                }
            }
        }
    }

    tasks.matching { task -> task.name == "check" }.configureEach {
        dependsOn(
            rootProject.tasks.named("verifyDesignTokens"),
            rootProject.tasks.named("verifyKmpTestTargets"),
            rootProject.tasks.named("verifyKmpAndroidCompilerFlags"),
            rootProject.tasks.named("verifyKmpDependencyCompatibility"),
            rootProject.tasks.named("verifyKmpConventionPlugins"),
        )
    }
}

gradle.projectsEvaluated {
    val kmpProjects = subprojects.filter { subproject ->
        subproject.pluginManager.hasPlugin("com.android.kotlin.multiplatform.library")
    }
    val commonTestModules = kmpProjects.filter { subproject ->
        !subproject.fileTree("src/commonTest/kotlin") { include("**/*.kt") }.isEmpty
    }.map(Project::getPath)
    val hostTestModules = kmpProjects.filter { subproject ->
        "testAndroidHostTest" in subproject.tasks.names
    }.map(Project::getPath)

    verifyKmpTestTargets.configure {
        this.commonTestModules.set(commonTestModules)
        this.hostTestModules.set(hostTestModules)
    }
    testAndroidHostTest.configure {
        dependsOn(
            kmpProjects.mapNotNull { subproject ->
                subproject.tasks.findByName("testAndroidHostTest")
            },
        )
    }

    val dependencyFixture = project(":kmp-convention-fixtures:plain")
    val commonCompatibilityConfiguration = dependencyFixture.configurations.getByName("kmpCompatibilityCommon")
    val androidCompatibilityConfiguration = dependencyFixture.configurations.getByName("kmpCompatibilityAndroid")
    verifyKmpDependencyCompatibility.configure {
        commonArtifacts.from(commonCompatibilityConfiguration)
        androidArtifacts.from(androidCompatibilityConfiguration)
        commonSelections.set(
            providers.provider {
                commonCompatibilityConfiguration.incoming.resolutionResult.root.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .map { dependency ->
                        "${dependency.requested.displayName} -> ${dependency.resolvedVariant.displayName}"
                    }
                    .sorted()
            },
        )
        androidSelections.set(
            providers.provider {
                androidCompatibilityConfiguration.incoming.resolutionResult.root.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .map { dependency ->
                        "${dependency.requested.displayName} -> ${dependency.resolvedVariant.displayName}"
                    }
                    .sorted()
            },
        )
    }

    val conventionFixturePaths = listOf(
        ":kmp-convention-fixtures:plain",
        ":kmp-convention-fixtures:compose",
    )
    val conventionFixtures = conventionFixturePaths.map(::project)
    val missingConventionCompileTasks = conventionFixtures.flatMap { fixture ->
        listOf("compileAndroidMain", "assembleAndroidMain")
            .filterNot(fixture.tasks.names::contains)
            .map { taskName -> "${fixture.path}:$taskName" }
    }
    val missingConventionVariants = conventionFixtures.flatMap { fixture ->
        listOf("androidApiElements", "androidRuntimeElements")
            .filter { configurationName ->
                fixture.configurations.findByName(configurationName)?.isCanBeConsumed != true
            }
            .map { configurationName -> "${fixture.path}:$configurationName" }
    }
    val composeFixture = project(":kmp-convention-fixtures:compose")
    val composeFixtureAndroidTarget = composeFixture.extensions
        .getByType(KotlinMultiplatformExtension::class.java)
        .targets
        .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        .single()
    verifyKmpConventionPlugins.configure {
        fixtureModules.set(conventionFixturePaths)
        missingCompileTasks.set(missingConventionCompileTasks)
        missingConsumableVariants.set(missingConventionVariants)
        composeFixturesMissingAndroidFlag.set(
            if ("-Xlambdas=class" in composeFixtureAndroidTarget.compilerOptions.freeCompilerArgs.get()) {
                emptyList()
            } else {
                listOf(composeFixture.path)
            },
        )
    }

    val applicableCompileTasks = mutableListOf<String>()
    val missingFlagCompileTasks = mutableListOf<String>()
    kmpProjects
        .filter { subproject ->
            subproject.pluginManager.hasPlugin("org.jetbrains.compose") &&
                    !subproject.path.startsWith(":kmp-convention-fixtures:")
        }
        .forEach { subproject ->
            val androidTarget = subproject.extensions
                .getByType(KotlinMultiplatformExtension::class.java)
                .targets
                .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                .single()
            subproject.tasks.names
                .filter { taskName ->
                    taskName == "compileAndroidMain" ||
                            taskName == "compileAndroidHostTest" ||
                            taskName == "compileAndroidDeviceTest"
                }
                .forEach { taskName ->
                    val taskPath = "${subproject.path}:$taskName"
                    applicableCompileTasks += taskPath
                    if ("-Xlambdas=class" !in androidTarget.compilerOptions.freeCompilerArgs.get()) {
                        missingFlagCompileTasks += taskPath
                    }
                }
        }
    verifyKmpAndroidCompilerFlags.configure {
        this.applicableCompileTasks.set(applicableCompileTasks)
        this.missingFlagCompileTasks.set(missingFlagCompileTasks)
    }
}
