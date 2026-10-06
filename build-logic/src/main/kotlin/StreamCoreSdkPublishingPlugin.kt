import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/** Local candidate publishing only. Release registry credentials are deliberately not configured. */
class StreamCoreSdkPublishingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val artifactBase = when (path) {
                ":sdk:model" -> "sdk-model"
                ":sdk:api" -> "sdk-api"
                ":sdk:runtime" -> "sdk-runtime"
                ":sdk:ui" -> "sdk-ui"
                ":sdk:providers:tmdb" -> "provider-tmdb"
                ":sdk:providers:clientB" -> "provider-clientb"
                ":sdk:providers:tmdb:ui" -> "provider-tmdb-ui"
                ":sdk:providers:clientB:ui" -> "provider-clientb-ui"
                else -> throw GradleException("Unregistered SDK publication: $path")
            }
            val optionalUi = path in OptionalUiProjects
            val stableAndroidModuleName = when (path) {
                ":sdk:providers:tmdb", ":sdk:providers:clientB" -> "data"
                ":sdk:ui", ":sdk:providers:tmdb:ui", ":sdk:providers:clientB:ui" -> "ui"
                else -> null
            }
            // Gradle's local component identity uses project group/name, not Maven artifactId.
            // All optional UI projects are named "ui"; unique internal groups prevent self-substitution.
            group = if (optionalUi) "$PublishedGroup.internal.$artifactBase" else PublishedGroup
            version = "0.1.0-alpha02"
            pluginManager.apply("maven-publish")
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                extensions.configure(KotlinMultiplatformExtension::class.java) {
                    targets.configureEach {
                        if (platformType == KotlinPlatformType.wasm) {
                            val wasmTargetName = name
                            compilations.configureEach {
                                val compilationName = name
                                compileTaskProvider.configure {
                                    // Keep KLIB identity tied to the artifact, independent of Gradle project names.
                                    val uniqueModuleName = "$artifactBase-$wasmTargetName-$compilationName"
                                    (compilerOptions as KotlinJsCompilerOptions).apply {
                                        moduleName.set(uniqueModuleName)
                                        // KGP's commonJsAdditionalCompilerFlags respects an explicit
                                        // IR name; output moduleName alone does not alter unique_name.
                                        freeCompilerArgs.add("-Xir-module-name=com.pampoukidis.streamcore:$uniqueModuleName")
                                    }
                                }
                            }
                        }
                        if (name == "android") {
                            compilations.configureEach {
                                if (stableAndroidModuleName != null) {
                                    val compilationName = name
                                    compileTaskProvider.configure {
                                        // Preserve the original data/UI JVM identities independently.
                                        (compilerOptions as KotlinJvmCompilerOptions).moduleName.set(
                                            if (compilationName == "main") stableAndroidModuleName else "${stableAndroidModuleName}_$compilationName",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            extensions.configure(PublishingExtension::class.java) {
                repositories.maven {
                    name = "SdkStaging"
                    url = rootProject.file(
                        rootProject.providers.gradleProperty("sdkRepository").orElse("build/sdk-repository").get(),
                    ).toURI()
                }
                publications.withType(MavenPublication::class.java).configureEach {
                    groupId = PublishedGroup
                    val suffix = if (name == "kotlinMultiplatform") {
                        ""
                    } else {
                        "-${name.lowercase()}"
                    }
                    artifactId = "$artifactBase$suffix"
                    pom {
                        name.set("StreamCore $artifactBase")
                        description.set("Backend-agnostic StreamCore SDK candidate; Android and Kotlin/Wasm consumption.")
                    }
                }
            }

            // KGP assigns target defaults after publication creation; finalize after target setup.
            afterEvaluate {
                extensions.getByType(PublishingExtension::class.java).publications
                    .withType(MavenPublication::class.java).configureEach {
                        val suffix = when (name) {
                            "kotlinMultiplatform" -> ""
                            "wasmJs" -> "-wasm-js"
                            else -> "-${name.lowercase()}"
                        }
                        artifactId = "$artifactBase$suffix"
                        groupId = PublishedGroup
                    }
            }

            val dependencyGate = if (optionalUi) "verifySdkUiDependencies" else "verifySdkHeadlessDependencies"
            tasks.register(dependencyGate) {
                group = "verification"
                description = if (optionalUi) {
                    "Rejects application features, SDK runtime/providers, Koin, Material, Coil, and playback engines from optional SDK UI."
                } else {
                    "Rejects all UI, Koin, and playback engines in headless SDK compile/runtime dependencies."
                }
                notCompatibleWithConfigurationCache("Resolves the SDK's variant configurations at execution time.")
                doLast {
                    val violations = sortedSetOf<String>()
                    configurations.filter { configuration ->
                        val normalizedName = configuration.name.lowercase()
                        configuration.isCanBeResolved &&
                            "test" !in normalizedName &&
                            ("compileclasspath" in normalizedName || "runtimeclasspath" in normalizedName ||
                                "compileklibraries" in normalizedName)
                    }.forEach { configuration ->
                        configuration.incoming.resolutionResult.allComponents.forEach componentLoop@{ component ->
                            val projectId = component.id as? ProjectComponentIdentifier
                            if (projectId != null) {
                                val disallowed = if (optionalUi) {
                                    projectId.projectPath !in setOf(target.path, ":sdk:model", ":sdk:ui")
                                } else {
                                    projectId.projectPath in OptionalUiProjects || projectId.projectPath == ":sdk:testing"
                                }
                                if (disallowed) violations.add("${configuration.name}: ${projectId.projectPath}")
                                return@componentLoop
                            }
                            val id = component.id as? ModuleComponentIdentifier ?: return@componentLoop
                            val disallowed = if (optionalUi) {
                                isForbiddenOptionalUiDependency(id.group, id.module, artifactBase)
                            } else {
                                isRenderingDependency(id.group, id.module) ||
                                    id.group == "com.pampoukidis.streamcore" &&
                                    (isOptionalUiArtifact(id.module) || id.module in setOf("sdk-testing", "sdk-testing-android", "sdk-testing-wasm-js"))
                            }
                            if (disallowed) {
                                violations.add("${configuration.name}: ${id.group}:${id.module}:${id.version}")
                            }
                        }
                    }
                    if (violations.isNotEmpty()) {
                        throw GradleException("SDK ${if (optionalUi) "optional UI" else "headless"} dependency violations in ${target.path}:\n${violations.joinToString("\n")}")
                    }
                }
            }
        }
    }

    private fun isRenderingDependency(group: String, module: String): Boolean {
        return group.startsWith("androidx.compose") || group.startsWith("org.jetbrains.compose") ||
            group.startsWith("org.jetbrains.skiko") || group.startsWith("androidx.tv") ||
            group.startsWith("io.insert-koin") || group.startsWith("androidx.media3") ||
            group.startsWith("com.google.android.exoplayer") || module.contains("shaka", ignoreCase = true)
    }

    private fun isOptionalUiArtifact(module: String): Boolean {
        return OptionalUiArtifacts.any { module == it || module.startsWith("$it-") }
    }

    private fun isForbiddenOptionalUiDependency(group: String, module: String, artifactBase: String): Boolean {
        // Compose resources brings foundation/UI and their platform lifecycle/Skiko dependencies.
        // Direct lifecycle usage is rejected by the source gate; do not reject that resource closure.
        return group.startsWith("io.insert-koin") || group.startsWith("io.coil-kt") ||
            group.startsWith("io.ktor") || group.startsWith("androidx.datastore") ||
            group.startsWith("org.jetbrains.androidx.datastore") ||
            group.startsWith("androidx.compose.material") || group.startsWith("org.jetbrains.compose.material") ||
            group.startsWith("androidx.tv") || group.startsWith("androidx.media3") ||
            group.startsWith("com.google.android.exoplayer") || module.contains("shaka", ignoreCase = true) ||
            group == "com.pampoukidis.streamcore" &&
                setOf("sdk-model", "sdk-ui", artifactBase).none { module == it || module.startsWith("$it-") }
    }

    private companion object {
        const val PublishedGroup = "com.pampoukidis.streamcore"
        val OptionalUiProjects = setOf(":sdk:ui", ":sdk:providers:tmdb:ui", ":sdk:providers:clientB:ui")
        val OptionalUiArtifacts = setOf("sdk-ui", "provider-tmdb-ui", "provider-clientb-ui")
    }
}
