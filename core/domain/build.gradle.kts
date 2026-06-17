plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(projects.core.data)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
