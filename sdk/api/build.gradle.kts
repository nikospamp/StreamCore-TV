plugins {
    id("streamcore.kmp.library")
    id("streamcore.sdk.publishing")
}

streamCoreKmp {
    withWasmJs()
}

kotlin {
    sourceSets {
        remove(getByName("commonTest"))

        commonMain.dependencies {
            api(projects.sdk.model)
            api(libs.kotlinx.coroutines.core)
        }
    }
}
