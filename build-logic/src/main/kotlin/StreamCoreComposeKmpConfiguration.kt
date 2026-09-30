import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Shared Compose plugin and Android compiler setup for rendered UI and resource-only libraries. */
internal fun Project.configureStreamCoreComposeKmp() {
    pluginManager.apply("streamcore.kmp.library")
    pluginManager.apply("org.jetbrains.compose")
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    val extension = extensions.getByType(KotlinMultiplatformExtension::class.java)
    extension.targets
        .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        .configureEach {
            // This JVM-only option must reach Android main/host compilations, never metadata/Wasm.
            compilerOptions.freeCompilerArgs.add("-Xlambdas=class")
        }
}
