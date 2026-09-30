import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val publishedUiResources = layout.buildDirectory.dir("processedResources/wasmJs/main/composeResources")

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser {
            testTask {
                dependsOn("wasmJsProcessResources")
                inputs.dir(publishedUiResources)
                environment("STREAMCORE_PUBLISHED_UI_RESOURCES", publishedUiResources.get().asFile.absolutePath)
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            // No manually supplied model, SDK UI, or Compose resource dependencies.
            implementation("com.pampoukidis.streamcore:provider-tmdb-ui:0.1.0-alpha02")
            implementation("com.pampoukidis.streamcore:provider-clientb-ui:0.1.0-alpha02")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

extensions.getByType<WasmNodeJsEnvSpec>().apply {
    download.set(false)
    command.set(providers.gradleProperty("streamcoreNodeExecutable").orElse("node"))
}
