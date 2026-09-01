package com.pampoukidis.streamcoretv.core.tracing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PerformanceTracerTest {
    @Test
    fun disabledTracerDoesNotEvaluateLabelAndExecutesBlockOnce() {
        var labelEvaluations = 0
        var blockExecutions = 0

        val result = NoOpPerformanceTracer.traceIfEnabled(
            label = {
                labelEvaluations += 1
                "disabled"
            },
            block = {
                blockExecutions += 1
                "result"
            },
        )

        assertEquals("result", result)
        assertEquals(0, labelEvaluations)
        assertEquals(1, blockExecutions)
    }

    @Test
    fun noOpTracerNeverEnablesSectionsOrCounters() {
        NoOpPerformanceTracer.beginSection("ignored")
        NoOpPerformanceTracer.counter("ignored", 1L)
        NoOpPerformanceTracer.endSection()

        assertFalse(NoOpPerformanceTracer.enabled)
    }

    @Test
    fun enabledTracerWrapsBlockWithOneSection() {
        val tracer = RecordingPerformanceTracer(enabled = true)

        val result = tracer.traceIfEnabled(label = { "section" }) { 42 }

        assertEquals(42, result)
        assertEquals(listOf("begin:section", "end"), tracer.events)
    }

    @Test
    fun enabledTracerEndsSectionWhenBlockFails() {
        val tracer = RecordingPerformanceTracer(enabled = true)

        assertFailsWith<IllegalStateException> {
            tracer.traceIfEnabled(label = { "section" }) {
                error("failure")
            }
        }

        assertEquals(listOf("begin:section", "end"), tracer.events)
    }
}

private class RecordingPerformanceTracer(
    override val enabled: Boolean,
) : PerformanceTracer {
    val events = mutableListOf<String>()

    override fun beginSection(name: String) {
        events += "begin:$name"
    }

    override fun endSection() {
        events += "end"
    }

    override fun counter(name: String, value: Long) {
        events += "counter:$name:$value"
    }
}
