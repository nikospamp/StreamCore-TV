pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("build-logic")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            name = "KotlinBinaryenDistributions"
            patternLayout {
                artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.github.webassembly", "binaryen")
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "StreamCoreTV"
include(":app")
include(":benchmark")
include(":benchmark:ui-driver")
include(":baselineprofile")
include(":webApp")
include(":core:tracing")
include(":core:tracing-api")
include(":kmp-convention-fixtures:plain")
include(":kmp-convention-fixtures:compose")

// SDK
include(":sdk:model")
include(":sdk:api")
include(":sdk:runtime")
include(":sdk:testing")
include(":sdk:ui")
include(":sdk:providers:tmdb")
include(":sdk:providers:clientB")
include(":sdk:providers:tmdb:ui")
include(":sdk:providers:clientB:ui")

// Shared application UI
include(":core:ui")
include(":core:ui-web")

// Login
include(":feature:login:ui-common")
include(":feature:login:ui-mobile")
include(":feature:login:ui-tablet")
include(":feature:login:ui-tv")
include(":feature:login:ui-web")

// Profiles
include(":feature:profiles:ui-common")
include(":feature:profiles:ui-mobile")
include(":feature:profiles:ui-tablet")
include(":feature:profiles:ui-tv")
include(":feature:profiles:ui-web")

// Home
include(":feature:home:ui-common")
include(":feature:home:ui-mobile")
include(":feature:home:ui-tablet")
include(":feature:home:ui-tv")
include(":feature:home:ui-web")

// Search
include(":feature:search:ui-common")
include(":feature:search:ui-mobile")
include(":feature:search:ui-tablet")
include(":feature:search:ui-tv")
include(":feature:search:ui-web")

// Details
include(":feature:details:ui-common")
include(":feature:details:ui-mobile")
include(":feature:details:ui-tablet")
include(":feature:details:ui-tv")
include(":feature:details:ui-web")

// Library
include(":feature:library:ui-common")
include(":feature:library:ui-mobile")
include(":feature:library:ui-tablet")
include(":feature:library:ui-tv")
include(":feature:library:ui-web")

// Playback
include(":playback:api")
include(":playback:media3")
include(":playback:web")
include(":feature:player:ui-common")
include(":feature:player:ui-mobile")
include(":feature:player:ui-tv")
include(":feature:player:ui-web")
