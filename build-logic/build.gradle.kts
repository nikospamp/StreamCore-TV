plugins {
    `kotlin-dsl`
}

group = "com.pampoukidis.streamcoretv.buildlogic"

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.compose:compose-gradle-plugin:${libs.versions.composeMultiplatform.get()}")
}

gradlePlugin {
    plugins {
        register("streamCoreKmpLibrary") {
            id = "streamcore.kmp.library"
            implementationClass = "StreamCoreKmpLibraryPlugin"
        }
        register("streamCoreComposeKmpLibrary") {
            id = "streamcore.kmp.compose.library"
            implementationClass = "StreamCoreComposeKmpLibraryPlugin"
        }
    }
}
