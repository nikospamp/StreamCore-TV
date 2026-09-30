import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec

plugins {
    id("streamcore.kmp.compose.library")
}

val profileUiBrowserResources = layout.buildDirectory.dir("processedResources/wasmJs/main/composeResources")

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser {
            testTask {
                dependsOn("wasmJsProcessResources")
                inputs.dir(profileUiBrowserResources)
                environment("STREAMCORE_PROFILE_UI_RESOURCES", profileUiBrowserResources.get().asFile.absolutePath)
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        // Match the player UI's executable Skiko test bundle (CMP-4906).
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.sdk.model)
            implementation(projects.core.ui)
            implementation(projects.core.uiWeb)
            api(projects.feature.profiles.uiCommon)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        wasmJsTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

extensions.getByType<WasmNodeJsEnvSpec>().apply {
    download.set(false)
    command.set(providers.gradleProperty("streamcoreNodeExecutable").orElse("node"))
}

dependencies {
    "androidRuntimeClasspath"(
        "org.jetbrains.compose.ui:ui-tooling:${libs.versions.composeMultiplatform.get()}"
    )
}
