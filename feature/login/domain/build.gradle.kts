plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    // Core
    implementation(projects.core.domain)
    implementation(projects.core.model)

    // Libraries
    implementation(libs.javax.inject)

    // Testing
    testImplementation(libs.junit)
}
