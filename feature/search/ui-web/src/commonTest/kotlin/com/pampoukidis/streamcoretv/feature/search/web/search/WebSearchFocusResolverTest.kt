package com.pampoukidis.streamcoretv.feature.search.web.search

import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureIds
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebSearchFocusResolverTest {
    @Test
    fun committedNativeValueIsDispatchedBeforeSubmit() {
        val actions = mutableListOf<SearchAction>()

        submitCommittedSearch(committedValue = "paste: value = still exact", onAction = actions::add)

        assertEquals(
            listOf(
                SearchAction.QueryChanged("paste: value = still exact"),
                SearchAction.SubmitQuery,
            ),
            actions,
        )
    }

    @Test
    fun resolvesTrendingAndResultTargetsByWholeFocusKey() {
        val discovery = WebSearchPreviewFixtures.discovery
        val trendingKey = discovery.trending[1].webSearchFocusKey()
        val results = WebSearchPreviewFixtures.state(WebBrowseFixtureScenario.Content)
        val resultItems = (results.content as SearchContentState.Results).items
        val resultKey = resultItems[2].webSearchFocusKey()

        assertEquals(
            WebSearchReturnFocusTarget.Trending(index = 1),
            resolveWebSearchReturnFocus(discovery, trendingKey),
        )
        assertEquals(
            WebSearchReturnFocusTarget.Results(index = 2),
            resolveWebSearchReturnFocus(results, resultKey),
        )
    }

    @Test
    fun waitsForLoadingAndFallsBackOnlyAfterTerminalState() {
        val key = WebBrowseFocusKey(
            destination = WebBrowseDestination.Search,
            sectionKey = "search:missing",
            itemKey = "missing",
        )
        val loading = SearchUiState(content = SearchContentState.Loading)
        val empty = SearchUiState(content = SearchContentState.Empty("missing"))

        assertEquals(
            WebSearchReturnFocusTarget.Pending,
            resolveWebSearchReturnFocus(loading, key),
        )
        assertEquals(
            WebSearchReturnFocusTarget.Field,
            resolveWebSearchReturnFocus(empty, key),
        )
    }

    @Test
    fun ignoresFocusForAnotherBrowseDestination() {
        val key = WebBrowseFocusKey(
            destination = WebBrowseDestination.Home,
            sectionKey = "featured",
            itemKey = "orbit-fall",
        )

        assertEquals(
            WebSearchReturnFocusTarget.None,
            resolveWebSearchReturnFocus(WebSearchPreviewFixtures.discovery, key),
        )
    }

    @Test
    fun allFrozenShowcasesAreBackendFreeAndRepresentTheirScenario() {
        val states = WebBrowseFixtureScenario.entries.associateWith(WebSearchPreviewFixtures::state)
        val allContent = states.values.flatMap { state ->
            buildList {
                addAll(state.trending)
                val contentState = state.content
                if (contentState is SearchContentState.Results) {
                    addAll(contentState.items)
                }
            }
        }

        assertIs<SearchContentState.Loading>(states.getValue(WebBrowseFixtureScenario.Loading).content)
        assertIs<SearchContentState.Results>(states.getValue(WebBrowseFixtureScenario.Content).content)
        assertIs<SearchContentState.Empty>(states.getValue(WebBrowseFixtureScenario.Empty).content)
        assertTrue(states.getValue(WebBrowseFixtureScenario.Offline).showOfflineNotice)
        assertIs<SearchContentState.Failure>(states.getValue(WebBrowseFixtureScenario.Error).content)
        assertTrue(states.getValue(WebBrowseFixtureScenario.LongText).query.length > 20)
        assertTrue(allContent.isNotEmpty())
        assertFalse(allContent.any { content -> content.poster.isNotBlank() || content.backdrop != null })
        assertTrue(
            allContent
                .filter { content -> content.row != WebBrowseFixtureIds.SearchTrendingSectionKey }
                .all { content -> content.row == WebBrowseFixtureIds.SearchResultsSectionKey },
        )
    }
}
