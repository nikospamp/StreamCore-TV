plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pampoukidis.streamcoretv"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.pampoukidis.streamcoretv"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "client"
    productFlavors {
        create("tmdb") {
            dimension = "client"
        }
        create("clientB") {
            dimension = "client"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

val tmdbImplementation by configurations
val clientBImplementation by configurations

dependencies {
    // Clients
    tmdbImplementation(projects.data.tmdb)
    clientBImplementation(projects.data.clientB)

    // Core
    implementation(projects.core.data)
    implementation(projects.core.ui)

    // Features
    implementation(projects.feature.login.uiMobile)
    implementation(projects.feature.login.uiTablet)
    implementation(projects.feature.login.uiTv)
    implementation(projects.feature.profiles.uiMobile)
    implementation(projects.feature.profiles.uiTablet)
    implementation(projects.feature.profiles.uiTv)
    implementation(projects.feature.home.uiMobile)
    implementation(projects.feature.home.uiTablet)
    implementation(projects.feature.home.uiTv)
    implementation(projects.feature.details.uiMobile)
    implementation(projects.feature.details.uiTablet)
    implementation(projects.feature.details.uiTv)

    // Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.dagger.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.dagger.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
