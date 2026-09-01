import com.android.build.api.attributes.AgpVersionAttr
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("streamcore.kmp.library")
}

kotlin {
    sourceSets.remove(sourceSets.getByName("commonTest"))
}

val kmpCompatibilityCommon by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("non-jvm"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-metadata"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
    }
}

val kmpCompatibilityAndroid by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    attributes {
        attribute(AgpVersionAttr.ATTRIBUTE, objects.named(libs.versions.agp.get()))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("android"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
    }
}

val lockedKmpDependencies = listOf(
    libs.androidx.datastore.core.okio,
    libs.coil.network.ktor3,
    libs.compose.components.resources,
    libs.compose.foundation,
    libs.compose.runtime,
    libs.compose.ui,
    libs.jetbrains.lifecycle.runtime.compose,
    libs.jetbrains.lifecycle.viewmodel.compose,
    libs.koin.compose,
    libs.koin.compose.viewmodel,
    libs.ktor.client.core,
    libs.ktor.client.content.negotiation,
    libs.ktor.serialization.kotlinx.json,
    libs.kotlinx.datetime,
)

dependencies {
    lockedKmpDependencies.forEach { dependencyProvider ->
        add(kmpCompatibilityCommon.name, dependencyProvider)
        add(kmpCompatibilityAndroid.name, dependencyProvider)
    }
}
