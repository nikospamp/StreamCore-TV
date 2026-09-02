plugins {
    id("streamcore.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

streamCoreKmp {
    withHostTest()
    // Transitive Compose UI types from :playback:api require Skiko's browser runtime.
    withWasmJs(withTests = false)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.playback.api)
            implementation(libs.androidx.datastore.core)
            implementation("androidx.datastore:datastore-preferences-core:1.2.1")
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.android)
        }
        wasmJsMain.dependencies {
            implementation(libs.androidx.datastore.core.okio.web)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
