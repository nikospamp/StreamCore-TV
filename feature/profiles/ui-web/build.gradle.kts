plugins {
    id("streamcore.kmp.compose.library")
}

streamCoreKmp {
    withWasmJs()
}

kotlin {
    sourceSets {
        remove(getByName("commonTest"))

        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.ui)
            implementation(projects.core.uiWeb)
            api(projects.feature.profiles.uiCommon)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.koin.compose.viewmodel)
        }
    }
}
