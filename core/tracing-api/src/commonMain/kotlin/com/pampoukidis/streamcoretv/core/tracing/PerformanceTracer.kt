package com.pampoukidis.streamcoretv.core.tracing

/** Platform tracing primitives. Trace labels must not contain user data. */
interface PerformanceTracer {
    val enabled: Boolean

    fun beginSection(name: String)

    fun endSection()

    fun counter(name: String, value: Long)
}

/** Shared/test tracer that performs no platform tracing work. */
object NoOpPerformanceTracer : PerformanceTracer {
    override val enabled: Boolean = false

    override fun beginSection(name: String) {
        return
    }

    override fun endSection() {
        return
    }

    override fun counter(name: String, value: Long) {
        return
    }
}

/**
 * Traces [block] only when this tracer is enabled.
 *
 * The function is inline and checks [PerformanceTracer.enabled] before evaluating [label], keeping the
 * disabled call path free of label interpolation and captured-lambda allocation.
 */
inline fun <T> PerformanceTracer.traceIfEnabled(
    label: () -> String,
    block: () -> T,
): T {
    if (!enabled) {
        return block()
    }

    beginSection(label())
    try {
        return block()
    } finally {
        endSection()
    }
}
