plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.pampoukidis.streamcoretv.baselineprofile"
    compileSdk = 37
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("profile") {
            isDebuggable = true
            matchingFallbacks += "release"
        }
    }

    flavorDimensions += "client"
    productFlavors {
        create("tmdb") {
            dimension = "client"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(projects.benchmark.uiDriver)
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
}
