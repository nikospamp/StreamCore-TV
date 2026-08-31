package com.pampoukidis.streamcoretv.core.tracing

import android.os.Build
import android.os.Trace
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTagsAsResourceId

const val BenchmarkTracingEnabled: Boolean = BuildConfig.ENABLED

/** No user data in trace labels. The disabled branches inline out of normal app builds. */
inline fun <T> benchmarkTrace(name: String, block: () -> T): T {
    if (!BuildConfig.ENABLED) return block()
    Trace.beginSection(name)
    try {
        return block()
    } finally {
        Trace.endSection()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Modifier.benchmarkSemantics(): Modifier {
    if (!BuildConfig.ENABLED) return this
    return semantics { testTagsAsResourceId = true }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Modifier.benchmarkReadiness(screen: String, ready: Boolean): Modifier {
    if (!BuildConfig.ENABLED) return this
    return semantics { contentDescription = "benchmark:$screen:${if (ready) "ready" else "loading"}" }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Modifier.benchmarkLayoutTrace(name: String): Modifier {
    if (!BuildConfig.ENABLED) return this
    return tracedLayout(name)
}

@PublishedApi
internal fun Modifier.tracedLayout(name: String): Modifier {
    val separator = name.lastIndexOf('.')
    val prefix = name.substring(0, separator)
    val owner = name.substring(separator + 1)
    val measureName = "$prefix.measure.$owner"
    val placeName = "$prefix.place.$owner"
    return layout { measurable, constraints ->
        val placeable = benchmarkTrace(measureName) {
            measurable.measure(constraints)
        }
        layout(placeable.width, placeable.height) {
            benchmarkTrace(placeName) {
                placeable.placeRelative(0, 0)
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun benchmarkCounter(name: String, value: Int) {
    if (BuildConfig.ENABLED && Build.VERSION.SDK_INT >= 29) {
        Trace.setCounter(name, value.toLong())
    }
}
