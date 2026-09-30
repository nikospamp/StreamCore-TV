import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    // Resource contracts only: keep shared Compose compiler setup without preview-renderer tooling.
    id("streamcore.kmp.resources.library")
    id("streamcore.sdk.publishing")
}

compose.resources {
    // Consuming presentation adapters and application previews reference these shared resources.
    publicResClass = true
    packageOfResClass = "com.pampoukidis.streamcore.sdk.ui.generated.resources"
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        namespace = "com.pampoukidis.streamcore.sdk.ui"
        androidResources.enable = true
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.sdk.model)
            api(libs.compose.components.resources)
            // Compose consumers need the runtime on their compiler classpath as well.
            api(libs.compose.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
