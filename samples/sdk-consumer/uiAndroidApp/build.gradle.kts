plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.streamcore.uiresources"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.example.streamcore.uiresources"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // The optional provider artifacts must expose model/contracts/resource API dependencies themselves.
    implementation("com.pampoukidis.streamcore:provider-tmdb-ui:0.1.0-alpha02")
    implementation("com.pampoukidis.streamcore:provider-clientb-ui:0.1.0-alpha02")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
