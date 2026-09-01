plugins {
    id("streamcore.kmp.compose.library")
}

kotlin {
    sourceSets.remove(sourceSets.getByName("commonTest"))
}
