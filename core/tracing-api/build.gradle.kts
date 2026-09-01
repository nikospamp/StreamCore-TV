plugins {
    id("streamcore.kmp.library")
}

kotlin {
    android {
        withHostTest {}
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
