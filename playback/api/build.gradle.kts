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
            api(projects.sdk.api)
            api(libs.kotlinx.coroutines.core)
            api(libs.compose.ui)
        }
    }
}
