plugins {
    id("streamcore.kmp.library")
}

kotlin {
    sourceSets {
        remove(getByName("commonTest"))

        commonMain.dependencies {
            api(projects.playback.api)
        }
    }
}
