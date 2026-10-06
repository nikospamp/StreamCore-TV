plugins {
    base
}

// Keeps shared test sources in a named Gradle project without compiling or publishing a library.
// Each provider includes src/commonTest/kotlin in its own commonTest; tests run in those providers.
