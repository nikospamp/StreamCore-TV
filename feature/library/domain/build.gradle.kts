plugins {
    id("streamcore.kmp.library")
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
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
