import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class StreamCoreKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")

            val extension = extensions.getByType(KotlinMultiplatformExtension::class.java)
            extension.targets
                .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                .configureEach {
                    namespace = streamCoreNamespace()
                    compileSdk = 36
                    minSdk = 24
                    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
                }

            tasks.withType(Test::class.java).configureEach {
                useJUnit()
            }
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
