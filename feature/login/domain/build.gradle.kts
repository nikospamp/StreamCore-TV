plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    // Core
    implementation(projects.core.domain)

    // Libraries
    implementation(libs.javax.inject)

    // Testing
    testImplementation(libs.junit)
}
