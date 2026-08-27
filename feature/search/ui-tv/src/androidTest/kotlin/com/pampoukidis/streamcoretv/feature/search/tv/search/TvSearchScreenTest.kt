package com.pampoukidis.streamcoretv.feature.search.tv.search

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchPreviewData
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvSearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchFieldReceivesInitialFocus() {
        setScreen()

        composeRule.onNodeWithTag(SearchTestTags.Field).assertIsFocused()
    }

    @Test
    fun recentSelectionDispatchesAction() {
        val actions = mutableListOf<SearchAction>()
        val query = SearchPreviewData.discovery.recentQueries.first()
        setScreen(onAction = actions::add)

        composeRule.onNodeWithTag(SearchTestTags.recent(query)).performClick()

        assertEquals(listOf(SearchAction.RecentSelected(query)), actions)
    }

    @Test
    fun resultSelectionDispatchesAction() {
        val actions = mutableListOf<SearchAction>()
        val content = SearchPreviewData.items.first().copy(row = "search:orbit")
        setScreen(state = SearchPreviewData.results, onAction = actions::add)

        composeRule.onNodeWithTag(SearchTestTags.result(content.id)).performClick()

        assertEquals(listOf(SearchAction.ResultSelected(content)), actions)
    }

    @Test
    fun loadingEmptyAndFailureStatesAreVisible() {
        setScreen(state = SearchPreviewData.loading)
        composeRule.onNodeWithTag(SearchTestTags.Loading).assertIsDisplayed()

        setScreen(state = SearchPreviewData.empty)
        composeRule.onNodeWithTag(SearchTestTags.Empty).assertIsDisplayed()

        setScreen(state = SearchPreviewData.failure)
        composeRule.onNodeWithTag(SearchTestTags.Failure).assertIsDisplayed()
    }

    private fun setScreen(
        state: SearchUiState = SearchPreviewData.discovery,
        onAction: (SearchAction) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(focusRequester) {
                    withFrameNanos { }
                    focusRequester.requestFocus()
                }
                TvSearchScreen(
                    state = state,
                    onAction = onAction,
                    onKeyboardRequested = {},
                    fieldFocusRequester = focusRequester,
                )
            }
        }
    }
}
