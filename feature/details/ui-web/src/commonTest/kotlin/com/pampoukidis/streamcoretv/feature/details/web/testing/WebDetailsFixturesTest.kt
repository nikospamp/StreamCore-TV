package com.pampoukidis.streamcoretv.feature.details.web.testing

import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebDetailsFixturesTest {

    @Test
    fun `fixture catalog covers all frozen scenarios`() {
        val loading = WebDetailsFixtures.state(WebBrowseFixtureScenario.Loading)
        val content = WebDetailsFixtures.state(WebBrowseFixtureScenario.Content)
        val empty = WebDetailsFixtures.state(WebBrowseFixtureScenario.Empty)
        val offline = WebDetailsFixtures.state(WebBrowseFixtureScenario.Offline)
        val error = WebDetailsFixtures.state(WebBrowseFixtureScenario.Error)
        val longText = WebDetailsFixtures.state(WebBrowseFixtureScenario.LongText)

        assertTrue(loading.isLoading)
        assertNotNull(content.content)
        assertTrue(content.recommendations.isNotEmpty())
        assertTrue(empty.recommendations.isEmpty())
        assertNotNull(offline.content)
        assertFalse(offline.isLibraryAvailable)
        assertFalse(error.isLoading)
        assertTrue(error.content == null)
        assertTrue(longText.content?.description.orEmpty().length > content.content?.description.orEmpty().length)
    }

    @Test
    fun `fixture artwork is backend free and never requests a remote image`() {
        WebBrowseFixtureScenario.entries.forEach { scenario ->
            val state = WebDetailsFixtures.state(scenario)
            val allContent = listOfNotNull(state.content) + state.recommendations

            assertTrue(allContent.all { content -> content.poster.isBlank() })
            assertTrue(allContent.all { content -> content.backdrop.isNullOrBlank() })
        }
    }
}
