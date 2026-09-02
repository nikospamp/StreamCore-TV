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
            api(projects.core.ui)
            implementation(projects.core.data)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
        }
    }
}
