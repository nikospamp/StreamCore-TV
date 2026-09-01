plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.pampoukidis.streamcoretv.benchmark.driver"
    compileSdk = 37
    targetProjectPath = ":app"
    defaultConfig {
        minSdk = 29
        targetSdk = 36
        ndk { abiFilters += providers.gradleProperty("benchmarkAbi").getOrElse("arm64-v8a") }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
    }
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
        create("benchmarkR8") {
            initWith(getByName("benchmark"))
        }
    }
    flavorDimensions += "client"
    productFlavors {
        create("tmdb") { dimension = "client" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enable = variant.buildType in setOf("benchmark", "benchmarkR8")
    }
}

dependencies {
    implementation(projects.benchmark.uiDriver)
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.tracing.perfetto)
    implementation(libs.androidx.tracing.perfetto.binary)
}
