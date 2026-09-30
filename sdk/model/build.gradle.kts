plugins {
    id("streamcore.kmp.library")
    id("streamcore.sdk.publishing")
    alias(libs.plugins.kotlin.serialization)
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
