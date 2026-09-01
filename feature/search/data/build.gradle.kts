plugins {
    id("streamcore.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.search.domain)
            implementation(libs.androidx.datastore.core)
            implementation("androidx.datastore:datastore-preferences-core:1.2.1")
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.android)
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
