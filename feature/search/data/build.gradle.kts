plugins {
    id("streamcore.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

streamCoreKmp {
    withHostTest()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.search.domain)
            implementation(libs.androidx.datastore.core)
            implementation("androidx.datastore:datastore-preferences-core:1.2.1")
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
