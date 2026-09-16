plugins {
    id("streamcore.kmp.compose.library")
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui.tooling.preview)
            implementation(projects.core.domain)
            implementation(projects.core.data)
            implementation(projects.core.tracingApi)
            api(projects.feature.home.domain)
            implementation(projects.playback.api)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
