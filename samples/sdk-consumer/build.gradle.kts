import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

plugins {
    id("com.android.application") version "9.1.1" apply false
    kotlin("multiplatform") version "2.3.21" apply false
    id("org.jetbrains.compose") version "1.12.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}

// Configure the root tooling environment as well as the target's environment.
gradle.projectsEvaluated {
    allprojects.forEach { consumer ->
        consumer.extensions.findByType(WasmNodeJsEnvSpec::class.java)?.apply {
            download.set(false)
            command.set(providers.gradleProperty("streamcoreNodeExecutable").orElse("node"))
        }
        consumer.extensions.findByType(WasmYarnRootEnvSpec::class.java)?.apply {
            download.set(false)
            command.set("yarn")
        }
    }
}

tasks.register("verifyCoordinateOnlyConsumption") {
    group = "verification"
    doLast {
        check(gradle.includedBuilds.isEmpty()) { "Composite substitution is forbidden in the publication consumer." }
        subprojects.forEach { consumer ->
            consumer.configurations.forEach { configuration ->
                check(configuration.dependencies.filterIsInstance<ProjectDependency>().all {
                    // AGP adds a test-to-own-application edge, not an SDK project dependency.
                    configuration.name.contains("test", ignoreCase = true) && it.path == consumer.path
                }) {
                    "Project dependencies are forbidden in ${consumer.path}:${configuration.name}"
                }
            }
        }
    }
}
