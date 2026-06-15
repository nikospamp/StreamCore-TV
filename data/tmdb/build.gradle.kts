import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.isFile) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

fun propertyOrLocalValue(name: String): String {
    return providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: ""
}

fun String.asBuildConfigString(): String {
    return "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

android {
    namespace = "com.pampoukidis.streamcoretv.data.tmdb"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "TMDB_BASE_URL",
            "https://api.themoviedb.org".asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "TMDB_READ_ACCESS_TOKEN",
            propertyOrLocalValue("tmdbReadAccessToken").asBuildConfigString(),
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(projects.core.domain)
    implementation(projects.core.data)

    // Libraries
    implementation(libs.androidx.annotation.experimental)
    implementation(libs.dagger.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    ksp(libs.dagger.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
