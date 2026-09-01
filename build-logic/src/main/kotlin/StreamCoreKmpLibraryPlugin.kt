import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec

class StreamCoreKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")

            val kotlinExtension = extensions.getByType(KotlinMultiplatformExtension::class.java)
            val androidTarget = kotlinExtension.targets
                .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                .named("android")
                .get()
            androidTarget.namespace = streamCoreNamespace()
            androidTarget.compileSdk = 37
            androidTarget.minSdk = 24
            androidTarget.compilerOptions.jvmTarget.set(JvmTarget.JVM_11)

            extensions.add(
                "streamCoreKmp",
                StreamCoreKmpExtension(
                    project = this,
                    androidTarget = androidTarget,
                    kotlinExtension = kotlinExtension,
                ),
            )

            tasks.withType(Test::class.java).configureEach {
                useJUnit()
            }
        }
    }
}

class StreamCoreKmpExtension internal constructor(
    private val project: Project,
    private val androidTarget: KotlinMultiplatformAndroidLibraryTarget,
    private val kotlinExtension: KotlinMultiplatformExtension,
) {
    fun withHostTest() {
        androidTarget.withHostTest {}
    }

    @OptIn(ExperimentalWasmDsl::class)
    fun withWasmJs() {
        val wasmTarget = kotlinExtension.wasmJs {
            nodejs()
        }
        wasmTarget.compilations.remove(wasmTarget.compilations.getByName("test"))
        project.tasks.matching { task ->
            val normalizedName = task.name.lowercase()
            "wasmjs" in normalizedName && "test" in normalizedName
        }.configureEach {
            enabled = false
        }
        project.extensions.getByType(WasmNodeJsEnvSpec::class.java).apply {
            download.set(false)
            command.set("node")
        }
    }
}

private fun Project.streamCoreNamespace(): String {
    val suffix = path
        .removePrefix(":")
        .split(':')
        .joinToString(".") { segment -> segment.replace("-", "") }
    return "com.pampoukidis.streamcoretv.$suffix"
}
