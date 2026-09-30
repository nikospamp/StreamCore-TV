plugins {
    id("streamcore.kmp.compose.library")
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    targets.withType<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sdk.api)
            api(projects.sdk.model)
            implementation(projects.core.ui)
            api(projects.playback.api)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.components.resources)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.http)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.pampoukidis.streamcoretv.feature.details.common.resources"
}
