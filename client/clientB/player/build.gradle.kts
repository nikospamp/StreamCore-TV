plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.pampoukidis.streamcoretv.client.clientb.player"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    implementation(projects.playback.api)
    implementation(projects.playback.media3)
    implementation(libs.dagger.hilt.android)
    implementation(libs.javax.inject)
    ksp(libs.dagger.hilt.compiler)
}
