import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class StreamCoreComposeKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("streamcore.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
            val composeVersion = libs.findVersion("composeMultiplatform").get().requiredVersion
            // The preview renderer needs tooling in each owning module. Android-KMP has no
            // debug variant; this local classpath keeps tooling out of published dependencies.
            dependencies.add(
                "androidRuntimeClasspath",
                "org.jetbrains.compose.ui:ui-tooling:$composeVersion",
            )

            val extension = extensions.getByType(KotlinMultiplatformExtension::class.java)
            extension.targets
                .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                .configureEach {
                    compilerOptions.freeCompilerArgs.add("-Xlambdas=class")
                }
        }
    }
}
