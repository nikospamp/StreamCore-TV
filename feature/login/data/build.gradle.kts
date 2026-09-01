plugins {
    id("streamcore.kmp.library")
}

kotlin {
    sourceSets {
        remove(getByName("commonTest"))
    }
}
