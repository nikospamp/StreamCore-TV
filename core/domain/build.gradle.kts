plugins {
    id("streamcore.kmp.library")
}

kotlin {
    sourceSets {
        remove(getByName("commonTest"))

        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
