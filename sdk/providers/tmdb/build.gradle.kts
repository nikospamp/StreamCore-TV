import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("streamcore.kmp.library")
    id("streamcore.sdk.publishing")
    alias(libs.plugins.kotlin.serialization)
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        // The SDK namespace follows the public provider package.
        namespace = "com.pampoukidis.streamcore.sdk.providers.tmdb"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.sdk.api)
            implementation(projects.sdk.runtime)
            implementation(projects.sdk.model)
            implementation(libs.androidx.datastore.core)
            implementation("androidx.datastore:datastore-preferences-core:1.2.1")
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.ktor.client.okhttp)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.androidx.datastore.core.okio.web)
        }
        // Compile the shared provider checks as test sources, outside published SDK artifacts.
        commonTest {
            kotlin.srcDir(rootProject.layout.projectDirectory.dir("sdk/testing/src/commonTest/kotlin"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
