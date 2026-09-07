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
            implementation(projects.core.data)
            implementation(projects.core.ui)
            implementation(projects.core.uiWeb)
            api(projects.feature.search.uiCommon)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    "androidRuntimeClasspath"(
        "org.jetbrains.compose.ui:ui-tooling:${libs.versions.composeMultiplatform.get()}"
    )
}
