import java.util.Properties

plugins {
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.isFile) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

val linkedLocalProperties = Properties()
val linkedLocalPropertiesFile = providers
    .gradleProperty("streamcoreLocalPropertiesPath")
    .orElse(providers.environmentVariable("STREAMCORE_LOCAL_PROPERTIES"))
    .orNull
    ?.let(rootProject::file)
if (linkedLocalPropertiesFile?.isFile == true) {
    linkedLocalPropertiesFile.inputStream().use { input ->
        linkedLocalProperties.load(input)
    }
}

fun propertyOrLocalValue(name: String): String {
    return providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: linkedLocalProperties.getProperty(name)
        ?: ""
}

fun String.asBuildConfigString(): String {
    return "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

val tmdbReadAccessToken = propertyOrLocalValue("tmdbReadAccessToken")
val tmdbAccountId = propertyOrLocalValue("tmdbAccountId")
val verifyTmdbRuntimeConfig = tasks.register("verifyTmdbRuntimeConfig") {
    group = "verification"
    description = "Fails when authenticated TMDB runtime configuration is missing. Values are never logged."
    inputs.property("tmdbReadAccessTokenConfigured", tmdbReadAccessToken.isNotBlank())
    inputs.property("tmdbAccountIdConfigured", tmdbAccountId.isNotBlank())

    doLast {
        check(inputs.properties["tmdbReadAccessTokenConfigured"] == true) {
            "Missing tmdbReadAccessToken. Use ignored local.properties, " +
                    "-PstreamcoreLocalPropertiesPath=<path>, or STREAMCORE_LOCAL_PROPERTIES."
        }
        check(inputs.properties["tmdbAccountIdConfigured"] == true) {
            "Missing tmdbAccountId. Use ignored local.properties, " +
                    "-PstreamcoreLocalPropertiesPath=<path>, or STREAMCORE_LOCAL_PROPERTIES."
        }
        logger.lifecycle("TMDB runtime configuration preflight passed (values redacted).")
    }
}

if (providers.gradleProperty("requireTmdbRuntimeConfig").orNull?.toBooleanStrictOrNull() == true) {
    tasks.configureEach {
        if (name.startsWith("preTmdb") && name.endsWith("Build")) {
            dependsOn(verifyTmdbRuntimeConfig)
        }
    }
}

android {
    namespace = "com.pampoukidis.streamcoretv"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pampoukidis.streamcoretv"
        minSdk = 26
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
        // Production-signing-equivalent rollout candidate. The shipping release
        // flag remains unchanged until all client smoke checks are complete.
        create("releaseR8") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += "release"
        }
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".benchmark"
            matchingFallbacks += "release"
        }
        create("benchmarkR8") {
            initWith(getByName("benchmark"))
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += listOf("benchmark", "release")
        }
        // Dedicated profile-generation target. It retains the isolated benchmark
        // application id/auth state without changing the frozen benchmark variants.
        create("profile") {
            initWith(getByName("benchmark"))
            matchingFallbacks += listOf("benchmark", "release")
        }
    }
    sourceSets.getByName("benchmarkR8").manifest.srcFile("src/benchmark/AndroidManifest.xml")
    sourceSets.getByName("profile").manifest.srcFile("src/benchmark/AndroidManifest.xml")

    flavorDimensions += "client"
    productFlavors {
        create("tmdb") {
            dimension = "client"
            buildConfigField(
                "String",
                "TMDB_BASE_URL",
                "https://api.themoviedb.org".asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "TMDB_READ_ACCESS_TOKEN",
                tmdbReadAccessToken.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "TMDB_ACCOUNT_ID",
                tmdbAccountId.asBuildConfigString(),
            )
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
        buildConfig = true
        compose = true
    }
}

baselineProfile {
    saveInSrc = true
    automaticGenerationDuringBuild = false
    variants {
        create("tmdbProfile") {
            from(project(":baselineprofile"))
            mergeIntoMain = true
        }
    }
}

val tmdbImplementation by configurations
val clientBImplementation by configurations

dependencies {
    // Required for reliable local/sideload profile installation and the
    // Macrobenchmark profile-install broadcast used by BaselineProfileMode.Require.
    implementation(libs.androidx.profileinstaller)
    listOf("benchmarkImplementation", "benchmarkR8Implementation", "profileImplementation").forEach { configuration ->
        add(configuration, libs.androidx.compose.runtime.tracing)
    }
    // Clients
    tmdbImplementation(projects.client.tmdb.data)
    tmdbImplementation(projects.client.tmdb.ui)
    tmdbImplementation(projects.client.tmdb.player)
    clientBImplementation(projects.client.clientB.data)
    clientBImplementation(projects.client.clientB.ui)
    clientBImplementation(projects.client.clientB.player)

    // Core
    implementation(projects.core.data)
    implementation(projects.core.domain)
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
    implementation(projects.feature.search.data)
    implementation(projects.feature.search.uiMobile)
    implementation(projects.feature.search.uiTablet)
    implementation(projects.feature.search.uiTv)
    implementation(projects.feature.details.uiMobile)
    implementation(projects.feature.details.uiTablet)
    implementation(projects.feature.details.uiTv)
    implementation(projects.feature.library.data)
    implementation(projects.feature.library.uiMobile)
    implementation(projects.feature.library.uiTablet)
    implementation(projects.feature.library.uiTv)
    implementation(projects.feature.player.data)
    implementation(projects.feature.player.uiMobile)
    implementation(projects.feature.player.uiTv)
    implementation(projects.playback.api)
    implementation(projects.playback.media3)

    // Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.koin.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.coil.compose)
    androidTestImplementation(libs.coil.network.ktor3)
    androidTestImplementation(libs.ktor.client.okhttp)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
