plugins {
    id("streamcore.kmp.library")
}

streamCoreKmp {
    withHostTest()
    // Transitive Compose UI types from :playback:api require Skiko's browser runtime.
    withWasmJs(withTests = false)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.playback.api)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
