package com.pampoukidis.streamcoretv.feature.home.web.testing

import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebHomePreviewDataTest {
    @Test
    fun loadingFixtureHasNoBackendContent() {
        val state = WebHomePreviewData.stateFor(WebBrowseFixtureScenario.Loading)

        assertTrue(state.isLoading)
        assertTrue(state.rows.isEmpty())
    }

    @Test
    fun contentAndOfflineFixturesUseBackendFreeArtwork() {
        val states = listOf(
            WebHomePreviewData.stateFor(WebBrowseFixtureScenario.Content),
            WebHomePreviewData.stateFor(WebBrowseFixtureScenario.Offline),
        )

        states.forEach { state ->
            assertFalse(state.isLoading)
            assertTrue(state.rows.isNotEmpty())
            assertTrue(state.rows.flatMap { row -> row.content }.all { content ->
                content.poster.isBlank() && content.backdrop == null
            })
        }
    }

    @Test
    fun emptyAndErrorFixturesDoNotInventContent() {
        val scenarios = listOf(
            WebBrowseFixtureScenario.Empty,
            WebBrowseFixtureScenario.Error,
        )

        scenarios.forEach { scenario ->
            val state = WebHomePreviewData.stateFor(scenario)
            assertFalse(state.isLoading)
            assertTrue(state.rows.isEmpty())
        }
    }

    @Test
    fun longTextFixturePreservesRowAndItemCounts() {
        val content = WebHomePreviewData.stateFor(WebBrowseFixtureScenario.Content)
        val longText = WebHomePreviewData.stateFor(WebBrowseFixtureScenario.LongText)

        assertEquals(content.rows.size, longText.rows.size)
        assertEquals(
            content.rows.sumOf { row -> row.content.size },
            longText.rows.sumOf { row -> row.content.size },
        )
        assertTrue(longText.rows.first().title.length > content.rows.first().title.length)
    }

    @Test
    fun everyFrozenScenarioProducesDeterministicState() {
        WebBrowseFixtureScenario.entries.forEach { scenario ->
            assertEquals(
                WebHomePreviewData.stateFor(scenario),
                WebHomePreviewData.stateFor(scenario),
            )
        }
    }
}
