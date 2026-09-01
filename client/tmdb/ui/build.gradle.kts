plugins {
    id("streamcore.kmp.compose.library")
}

streamCoreKmp {
    withHostTest()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(projects.core.data)
            implementation(projects.core.ui)
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
