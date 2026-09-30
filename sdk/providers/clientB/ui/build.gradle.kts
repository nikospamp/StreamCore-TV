plugins {
    // Resource resolvers use shared Compose compiler setup without preview-renderer tooling.
    id("streamcore.kmp.resources.library")
    id("streamcore.sdk.publishing")
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    targets.withType<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        namespace = "com.pampoukidis.streamcore.sdk.providers.clientb.ui"
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.sdk.ui)
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    packageOfResClass = "com.pampoukidis.streamcore.sdk.providers.clientb.ui.generated.resources"
}
