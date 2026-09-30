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
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.sdk.api)
            api(libs.androidx.datastore.core)
            api("androidx.datastore:datastore-preferences-core:1.2.1")
            api(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
        }
        getByName("androidHostTest").dependencies {
            // FileStorage uses File.renameTo, which cannot replace existing files on Windows.
            // Exercise the identical Preferences protobuf codec with portable atomic file writes.
            implementation(libs.androidx.datastore.core.okio)
        }
        wasmJsMain.dependencies {
            implementation(libs.androidx.datastore.core.okio.web)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
