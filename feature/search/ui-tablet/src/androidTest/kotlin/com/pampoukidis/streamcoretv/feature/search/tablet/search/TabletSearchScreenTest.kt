package com.pampoukidis.streamcoretv.feature.search.tablet.search

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchPreviewData
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TabletSearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun queryInputAndClearDispatchActions() {
        val actions = mutableListOf<SearchAction>()
        setScreen(state = SearchPreviewData.results, onAction = actions::add)

        composeRule
            .onNodeWithTag(SearchTestTags.Field)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Search titles")
            .performTextInput(" station")
        composeRule
            .onNodeWithTag(SearchTestTags.ClearQuery)
            .assertContentDescriptionEquals("Clear search")
            .performClick()

        assertEquals(
            listOf(
                SearchAction.QueryChanged("orbit station"),
                SearchAction.ClearQuery,
            ),
            actions,
        )
    }

    @Test
    fun discoveryRecentsDispatchSelectionAndRemoval() {
        val actions = mutableListOf<SearchAction>()
        val query = SearchPreviewData.discovery.recentQueries.first()
        setScreen(state = SearchPreviewData.discovery, onAction = actions::add)

        composeRule.onNodeWithTag(SearchTestTags.removeRecent(query)).performClick()
        composeRule.onNodeWithTag(SearchTestTags.recent(query)).performClick()

        assertEquals(
            listOf(
                SearchAction.RecentRemoved(query),
                SearchAction.RecentSelected(query),
            ),
            actions,
        )
    }

    @Test
    fun resultSelectionDispatchesSelectedContent() {
        val actions = mutableListOf<SearchAction>()
        val item = (SearchPreviewData.results.content as
            com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState.Results)
            .items.first()
        setScreen(state = SearchPreviewData.results, onAction = actions::add)

        composeRule.onNodeWithTag(SearchTestTags.result(item.id)).performClick()

        assertEquals(listOf(SearchAction.ResultSelected(item)), actions)
    }

    @Test
    fun loadingEmptyAndFailureStatesExposeRecoveryUi() {
        var state by mutableStateOf(SearchPreviewData.loading)
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TabletSearchScreen(
                    state = state,
                    onAction = {},
                    gridState = rememberLazyGridState(),
                )
            }
        }
        composeRule.onNodeWithTag(SearchTestTags.Loading).assertIsDisplayed()

        composeRule.runOnIdle { state = SearchPreviewData.empty }
        composeRule.onNodeWithTag(SearchTestTags.Empty).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.Field).assertTextEquals("unknown title")
        composeRule.onNodeWithText("Clear search").assertIsDisplayed()

        composeRule.runOnIdle { state = SearchPreviewData.failure }
        composeRule.onNodeWithTag(SearchTestTags.Failure).assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    private fun setScreen(
        state: SearchUiState,
        onAction: (SearchAction) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TabletSearchScreen(
                    state = state,
                    onAction = onAction,
                    gridState = rememberLazyGridState(),
                )
            }
        }
    }
}
