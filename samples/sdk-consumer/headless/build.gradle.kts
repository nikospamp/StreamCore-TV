import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { nodejs() }
    sourceSets {
        commonMain.dependencies {
            implementation("com.pampoukidis.streamcore:provider-clientb:0.1.0-alpha02")
            implementation("com.pampoukidis.streamcore:provider-tmdb:0.1.0-alpha02")
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
