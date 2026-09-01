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

            val kotlinExtension = extensions.getByType(KotlinMultiplatformExtension::class.java)
            val androidTarget = kotlinExtension.targets
                .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                .named("android")
                .get()
            androidTarget.namespace = streamCoreNamespace()
            androidTarget.compileSdk = 36
            androidTarget.minSdk = 24
            androidTarget.compilerOptions.jvmTarget.set(JvmTarget.JVM_11)

            extensions.add(
                "streamCoreKmp",
                StreamCoreKmpExtension(androidTarget),
            )

            tasks.withType(Test::class.java).configureEach {
                useJUnit()
            }
        }
    }
}

class StreamCoreKmpExtension internal constructor(
    private val androidTarget: KotlinMultiplatformAndroidLibraryTarget,
) {
    fun withHostTest() {
        androidTarget.withHostTest {}
    }
}

private fun Project.streamCoreNamespace(): String {
    val suffix = path
        .removePrefix(":")
        .split(':')
        .joinToString(".") { segment -> segment.replace("-", "") }
    return "com.pampoukidis.streamcoretv.$suffix"
}
