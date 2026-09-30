plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.streamcore.consumer"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.example.streamcore.consumer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Deliberately no explicit SDK model/API/runtime, coroutine, storage, or transport dependencies.
    implementation("com.pampoukidis.streamcore:provider-clientb:0.1.0-alpha02")
    implementation("com.pampoukidis.streamcore:provider-tmdb:0.1.0-alpha02")
    testImplementation("junit:junit:4.13.2")
}
