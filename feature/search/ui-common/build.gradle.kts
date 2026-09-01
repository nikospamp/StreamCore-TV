plugins {
    id("streamcore.kmp.compose.library")
}

streamCoreKmp {
    withHostTest()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            api(projects.feature.search.domain)
            implementation(libs.compose.runtime)
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
