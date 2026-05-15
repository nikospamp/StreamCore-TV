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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "StreamCoreTV"
include(":app")

// Core
include(":core:model")
include(":core:domain")
include(":core:ui")

// Clients
include(":data:clientA")
include(":data:clientB")

// Login
include(":feature:login:data")
include(":feature:login:domain")
include(":feature:login:ui-common")
include(":feature:login:ui-mobile")
include(":feature:login:ui-tablet")
include(":feature:login:ui-tv")

// Profiles
include(":feature:profiles:data")
include(":feature:profiles:domain")
include(":feature:profiles:ui-common")
include(":feature:profiles:ui-mobile")
include(":feature:profiles:ui-tablet")
include(":feature:profiles:ui-tv")