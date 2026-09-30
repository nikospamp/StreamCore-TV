import org.gradle.api.Plugin
import org.gradle.api.Project

/** Compose Resources without the preview-renderer tooling used by application UI modules. */
class StreamCoreResourcesKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureStreamCoreComposeKmp()
    }
}
