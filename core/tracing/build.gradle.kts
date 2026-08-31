plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pampoukidis.streamcoretv.core.tracing"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        buildConfigField("boolean", "ENABLED", "false")
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        named("benchmark") { buildConfigField("boolean", "ENABLED", "true") }
        named("benchmarkR8") { buildConfigField("boolean", "ENABLED", "true") }
        named("profile") { buildConfigField("boolean", "ENABLED", "true") }
        named("nonMinifiedProfile") { buildConfigField("boolean", "ENABLED", "true") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
}
