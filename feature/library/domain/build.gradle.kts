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
            implementation(libs.koin.core)
            api(projects.core.data)
            api(projects.core.domain)
            api(projects.playback.api)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
