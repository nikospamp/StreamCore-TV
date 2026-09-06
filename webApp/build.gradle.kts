import groovy.json.JsonOutput
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

@DisableCachingByDefault(because = "Local browser configuration must not enter the shared build cache")
abstract class GenerateWebDevelopmentConfig : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val localPropertiesFiles: ConfigurableFileCollection

    @get:Input
    abstract val overrides: MapProperty<String, String>

    @get:OutputFile
    abstract val configFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val output = configFile.get().asFile
        // A failed preflight must not leave a previously valid configuration available.
        output.delete()
        val values = mutableMapOf<String, String>()
        localPropertiesFiles.forEach { file ->
            if (file.isFile) {
                val properties = Properties()
                file.inputStream().use { properties.load(it) }
                properties.stringPropertyNames().forEach { name ->
                    values.putIfAbsent(name, properties.getProperty(name))
                }
            }
        }
        values.putAll(overrides.get())
        val config = linkedMapOf(
            "tmdbBaseUrl" to (values["tmdbBaseUrl"] ?: "https://api.themoviedb.org/"),
            "tmdbReadAccessToken" to values["tmdbReadAccessToken"].orEmpty(),
            "tmdbAccountId" to values["tmdbAccountId"].orEmpty(),
        )
        val missing = config.filterValues { it.isBlank() }.keys
        check(missing.isEmpty()) {
            "Missing web development configuration: ${missing.joinToString()}. " +
                    "Set these in local.properties, Gradle properties, or a file selected by " +
                    "-PstreamcoreLocalPropertiesPath / STREAMCORE_LOCAL_PROPERTIES."
        }
        val baseUrl = runCatching { URI(config.getValue("tmdbBaseUrl")) }.getOrNull()
        check(baseUrl?.scheme == "https" && !baseUrl.host.isNullOrBlank()) {
            "tmdbBaseUrl must be an absolute HTTPS URL."
        }
        output.parentFile.mkdirs()
        output.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(config)) + "\n")
        logger.lifecycle("Web development configuration generated (values redacted).")
    }
}

val developmentConfigDirectory = layout.buildDirectory.dir("generated/webDevelopmentConfig")
val generateWebDevelopmentConfig = tasks.register<GenerateWebDevelopmentConfig>("generateWebDevelopmentConfig") {
    group = "development"
    description = "Generates browser runtime configuration from the existing local TMDB settings."
    localPropertiesFiles.from(rootProject.layout.projectDirectory.file("local.properties"))
    val linkedPropertiesPath = providers.gradleProperty("streamcoreLocalPropertiesPath")
        .orElse(providers.environmentVariable("STREAMCORE_LOCAL_PROPERTIES"))
    if (linkedPropertiesPath.isPresent) {
        localPropertiesFiles.from(rootProject.file(linkedPropertiesPath.get()))
    }
    overrides.convention(emptyMap())
    listOf("tmdbBaseUrl", "tmdbReadAccessToken", "tmdbAccountId").forEach { name ->
        val value = providers.gradleProperty(name)
        if (value.isPresent) {
            overrides.put(name, value)
        }
    }
    configFile.set(developmentConfigDirectory.map { it.file("config.json") })
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "streamcore-web.js"
            }
            runTask {
                if (mode == KotlinWebpackConfig.Mode.DEVELOPMENT) {
                    dependsOn(generateWebDevelopmentConfig)
                    devServerProperty.set(devServerProperty.get().apply {
                        static(developmentConfigDirectory.get().asFile.invariantSeparatorsPath)
                    })
                }
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain {
            // Runtime values are served by the dev server or deployment, never bundled.
            resources.exclude("config.json")
            resources.srcDir(
                project(":playback:web").file("src/wasmJsMain/resources"),
            )
            resources.srcDir(
                project(":feature:player:ui-web").file("src/wasmJsMain/resources"),
            )
        }
        wasmJsMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.domain)
            implementation(projects.core.tracingApi)
            implementation(projects.core.ui)
            implementation(projects.core.uiWeb)
            implementation(projects.client.tmdb.data)
            implementation(projects.client.tmdb.ui)
            implementation(projects.client.tmdb.player)
            implementation(projects.feature.login.domain)
            implementation(projects.feature.login.uiCommon)
            implementation(projects.feature.login.uiWeb)
            implementation(projects.feature.profiles.domain)
            implementation(projects.feature.profiles.uiCommon)
            implementation(projects.feature.profiles.uiWeb)
            implementation(projects.feature.home.domain)
            implementation(projects.feature.home.uiCommon)
            implementation(projects.feature.home.uiWeb)
            implementation(projects.feature.search.data)
            implementation(projects.feature.search.domain)
            implementation(projects.feature.search.uiCommon)
            implementation(projects.feature.search.uiWeb)
            implementation(projects.feature.details.domain)
            implementation(projects.feature.details.uiCommon)
            implementation(projects.feature.details.uiWeb)
            implementation(projects.feature.library.data)
            implementation(projects.feature.library.domain)
            implementation(projects.feature.library.uiCommon)
            implementation(projects.feature.library.uiWeb)
            implementation(projects.feature.player.data)
            implementation(projects.feature.player.uiCommon)
            implementation(projects.feature.player.uiWeb)
            implementation(projects.playback.api)
            implementation(projects.playback.web)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(compose.material3)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.androidx.datastore.core)
            implementation("androidx.datastore:datastore-preferences-core:${libs.versions.datastore.get()}")
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.js)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        wasmJsTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

extensions.configure<WasmNodeJsEnvSpec> {
    download.set(false)
    command.set("node")
}
