import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("streamcore.kmp.library")
    id("streamcore.sdk.publishing")
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        // The SDK namespace follows the public provider package.
        namespace = "com.pampoukidis.streamcore.sdk.providers.clientb"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.sdk.api)
            implementation(projects.sdk.runtime)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.sdk.model)
            implementation(libs.androidx.datastore.core)
            implementation("androidx.datastore:datastore-preferences-core:1.2.1")
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(projects.sdk.testing)
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
