plugins {
    id("streamcore.kmp.library")
}

streamCoreKmp {
    withWasmJs()
}

kotlin {
    sourceSets {
        remove(getByName("commonTest"))

        commonMain.dependencies {
            implementation(projects.core.data)
        }
    }
}
