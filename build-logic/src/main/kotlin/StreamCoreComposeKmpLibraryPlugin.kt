import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class StreamCoreComposeKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("streamcore.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.getByType(KotlinMultiplatformExtension::class.java)
            extension.targets
                .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                .configureEach {
                    compilerOptions.freeCompilerArgs.add("-Xlambdas=class")
                }
        }
    }
}
