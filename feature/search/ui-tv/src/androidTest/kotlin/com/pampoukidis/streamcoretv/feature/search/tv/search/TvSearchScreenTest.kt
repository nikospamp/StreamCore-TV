package com.pampoukidis.streamcoretv.feature.search.tv.search

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
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

        composeRule.onNodeWithTag(SearchTestTags.recent(query))
            .performSemanticsAction(SemanticsActions.RequestFocus) { requestFocus ->
                requestFocus()
            }
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()

        assertEquals(listOf(SearchAction.RecentSelected(query)), actions)
    }

    @Test
    fun resultSelectionDispatchesAction() {
        val actions = mutableListOf<SearchAction>()
        val content = SearchPreviewData.items.first().copy(
            row = "search:orbit",
            poster = "",
            backdrop = null,
        )
        setScreen(state = SearchPreviewData.results, onAction = actions::add)

        composeRule.onNodeWithTag(SearchTestTags.result(content.id)).performClick()

        assertEquals(listOf(SearchAction.ResultSelected(content)), actions)
    }

    @Test
    fun returnFocusKeyRestoresExactResultAndPreservesQuery() {
        val content = SearchPreviewData.items[3].copy(
            row = "search:orbit",
            poster = "",
            backdrop = null,
        )
        val consumedKeys = mutableListOf<String>()
        val returnFocusKey = com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
            .content(content.id, content.row)

        setScreen(
            state = SearchPreviewData.results,
            returnFocusKey = returnFocusKey,
            onReturnFocusConsumed = consumedKeys::add,
            requestInitialFieldFocus = false,
        )

        composeRule.onNodeWithTag(SearchTestTags.result(content.id)).assertIsFocused()
        composeRule.onNodeWithTag(SearchTestTags.Field).assertTextEquals("orbit")
        assertEquals(listOf(returnFocusKey), consumedKeys)
    }

    @Test
    fun missingReturnFocusKeyFallsBackToQueryField() {
        val consumedKeys = mutableListOf<String>()
        val missingKey = "missing:content"

        setScreen(
            state = SearchPreviewData.results,
            returnFocusKey = missingKey,
            onReturnFocusConsumed = consumedKeys::add,
            requestInitialFieldFocus = false,
        )

        composeRule.onNodeWithTag(SearchTestTags.Field).assertIsFocused()
        assertEquals(listOf(missingKey), consumedKeys)
    }

    @Test
    fun loadingEmptyAndFailureStatesAreVisible() {
        var state by mutableStateOf(SearchPreviewData.loading)
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvSearchScreen(
                    state = state,
                    onAction = {},
                    onKeyboardRequested = {},
                    fieldFocusRequester = remember { FocusRequester() },
                )
            }
        }
        composeRule.onNodeWithTag(SearchTestTags.Loading).assertIsDisplayed()

        composeRule.runOnIdle { state = SearchPreviewData.empty }
        composeRule.onNodeWithTag(SearchTestTags.Empty).assertIsDisplayed()

        composeRule.runOnIdle { state = SearchPreviewData.failure }
        composeRule.onNodeWithTag(SearchTestTags.Failure).assertIsDisplayed()
    }

    private fun setScreen(
        state: SearchUiState = SearchPreviewData.discovery,
        onAction: (SearchAction) -> Unit = {},
        returnFocusKey: String? = null,
        onReturnFocusConsumed: (String) -> Unit = {},
        requestInitialFieldFocus: Boolean = true,
    ) {
        val backendFreeState = state.copy(
            trending = state.trending.map { content ->
                content.copy(poster = "", backdrop = null)
            },
            content = when (val contentState = state.content) {
                is SearchContentState.Results -> contentState.copy(
                    items = contentState.items.map { content ->
                        content.copy(poster = "", backdrop = null)
                    },
                )

                else -> contentState
            },
        )
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(focusRequester) {
                    if (requestInitialFieldFocus) {
                        withFrameNanos { }
                        focusRequester.requestFocus()
                    }
                }
                TvSearchScreen(
                    state = backendFreeState,
                    onAction = onAction,
                    onKeyboardRequested = {},
                    fieldFocusRequester = focusRequester,
                    returnFocusKey = returnFocusKey,
                    onReturnFocusConsumed = onReturnFocusConsumed,
                )
            }
        }
    }
}
