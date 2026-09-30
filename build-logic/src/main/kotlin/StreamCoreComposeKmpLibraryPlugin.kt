import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

class StreamCoreComposeKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureStreamCoreComposeKmp()

            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
            val composeVersion = libs.findVersion("composeMultiplatform").get().requiredVersion
            // The preview renderer needs tooling in each owning module. Android-KMP has no
            // debug variant; this local classpath keeps tooling out of published dependencies.
            dependencies.add(
                "androidRuntimeClasspath",
                "org.jetbrains.compose.ui:ui-tooling:$composeVersion",
            )
        }
    }
}
