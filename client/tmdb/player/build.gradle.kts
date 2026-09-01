plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "com.pampoukidis.streamcoretv.client.tmdb.player"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    implementation(libs.koin.android)
    implementation(projects.playback.api)
    implementation(projects.playback.media3)
}
