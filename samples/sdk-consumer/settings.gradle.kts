pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "SdkStaging"
                    url = uri(providers.gradleProperty("sdkRepository").orElse("../../build/sdk-repository").get())
                }
            }
            filter { includeGroup("com.pampoukidis.streamcore") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "IndependentStreamCoreConsumers"
include(":androidApp", ":headless", ":uiAndroidApp", ":uiWasm")
