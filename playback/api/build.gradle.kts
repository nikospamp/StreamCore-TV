plugins {
    id("streamcore.kmp.compose.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        remove(getByName("commonTest"))

        commonMain.dependencies {
            api(projects.core.data)
            api(libs.kotlinx.coroutines.core)
            api(libs.compose.ui)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
