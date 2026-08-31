package com.pampoukidis.streamcoretv.benchmark

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

internal object BenchmarkTarget {
    const val PackageName = "com.pampoukidis.streamcoretv.benchmark"
    const val TimeoutMillis = 45_000L

    fun outputFile(name: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Match Macrobenchmark's app-and-shell-accessible output location on Android Q+.
        @Suppress("DEPRECATION")
        val externalFiles = checkNotNull(context.externalMediaDirs.firstOrNull())
        val configuredOutput = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val base = configuredOutput?.let { File(it) } ?: externalFiles
        val directory = File(base, "navigation").apply { mkdirs() }
        return File(directory, name)
    }
}
