package com.pampoukidis.streamcoretv.feature.library.web.testing

import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebLibraryFixturesTest {

    @Test
    fun `fixture catalog covers loading content empty offline error and long text`() {
        val loading = WebLibraryFixtures.state(WebBrowseFixtureScenario.Loading)
        val content = WebLibraryFixtures.state(WebBrowseFixtureScenario.Content)
        val empty = WebLibraryFixtures.state(WebBrowseFixtureScenario.Empty)
        val offline = WebLibraryFixtures.state(WebBrowseFixtureScenario.Offline)
        val error = WebLibraryFixtures.state(WebBrowseFixtureScenario.Error)
        val longText = WebLibraryFixtures.state(WebBrowseFixtureScenario.LongText)

        assertTrue(loading.isLoading)
        assertFalse(content.isLoading)
        assertTrue(content.continueWatching.isNotEmpty())
        assertTrue(content.likedContent.isNotEmpty())
        assertTrue(content.myListContent.isNotEmpty())
        assertTrue(empty.continueWatching.isEmpty())
        assertTrue(empty.likedContent.isEmpty())
        assertTrue(empty.myListContent.isEmpty())
        assertIs<AppError.Network>(offline.error)
        assertNotNull(error.error)
        assertTrue(longText.myListContent.first().title.length > content.myListContent.first().title.length)
    }

    @Test
    fun `all fixture artwork remains backend free`() {
        WebBrowseFixtureScenario.entries.forEach { scenario ->
            val state = WebLibraryFixtures.state(scenario)
            val allContent = state.continueWatching + state.likedContent + state.myListContent

            assertTrue(allContent.all { content -> content.poster.isBlank() })
            assertTrue(allContent.all { content -> content.backdrop.isNullOrBlank() })
        }
    }
}
