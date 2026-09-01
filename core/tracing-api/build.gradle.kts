plugins {
    id("streamcore.kmp.library")
}

streamCoreKmp {
    withHostTest()
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
