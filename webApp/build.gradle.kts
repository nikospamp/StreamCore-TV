import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "streamcore-web.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.domain)
            implementation(projects.core.tracingApi)
            implementation(projects.core.ui)
            implementation(projects.core.uiWeb)
            implementation(projects.client.tmdb.data)
            implementation(projects.client.tmdb.ui)
            implementation(projects.client.tmdb.player)
            implementation(projects.feature.login.domain)
            implementation(projects.feature.login.uiCommon)
            implementation(projects.feature.login.uiWeb)
            implementation(projects.feature.profiles.domain)
            implementation(projects.feature.profiles.uiCommon)
            implementation(projects.feature.profiles.uiWeb)
            implementation(projects.feature.home.domain)
            implementation(projects.feature.home.uiCommon)
            implementation(projects.feature.home.uiWeb)
            implementation(projects.feature.search.data)
            implementation(projects.feature.search.domain)
            implementation(projects.feature.search.uiCommon)
            implementation(projects.feature.search.uiWeb)
            implementation(projects.feature.details.domain)
            implementation(projects.feature.details.uiCommon)
            implementation(projects.feature.details.uiWeb)
            implementation(projects.feature.library.data)
            implementation(projects.feature.library.domain)
            implementation(projects.feature.library.uiCommon)
            implementation(projects.feature.library.uiWeb)
            implementation(projects.feature.player.data)
            implementation(projects.feature.player.uiCommon)
            implementation(projects.playback.api)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(compose.material3)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.androidx.datastore.core)
            implementation("androidx.datastore:datastore-preferences-core:${libs.versions.datastore.get()}")
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.js)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(npm("shaka-player", libs.versions.shaka.get()))
        }
        wasmJsTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

extensions.configure<WasmNodeJsEnvSpec> {
    download.set(false)
    command.set("node")
}
