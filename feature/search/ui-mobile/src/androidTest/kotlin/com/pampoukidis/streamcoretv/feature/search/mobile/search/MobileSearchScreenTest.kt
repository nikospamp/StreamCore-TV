package com.pampoukidis.streamcoretv.feature.search.mobile.search

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchPreviewData
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MobileSearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchField_hasAccessibleLabelAndExactHeight() {
        setContent(state = SearchPreviewData.discovery)

        composeRule.onNodeWithTag(SearchTestTags.Field)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Search titles")
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun clearQuery_dispatchesClearAction() {
        val actions = mutableListOf<SearchAction>()
        setContent(
            state = SearchPreviewData.results,
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(SearchTestTags.ClearQuery)
            .assertContentDescriptionEquals("Clear search")
            .performClick()

        assertEquals(listOf(SearchAction.ClearQuery), actions)
    }

    @Test
    fun recentSelectionAndRemoval_areDistinctActions() {
        val actions = mutableListOf<SearchAction>()
        val query = SearchPreviewData.discovery.recentQueries.first()
        setContent(
            state = SearchPreviewData.discovery,
            onAction = actions::add,
        )

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
    fun resultSelection_dispatchesSingleAction() {
        val actions = mutableListOf<SearchAction>()
        val item = SearchPreviewData.items.first()
        setContent(
            state = SearchPreviewData.results,
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(SearchTestTags.result(item.id)).performClick()

        assertEquals(listOf(SearchAction.ResultSelected(item.copy(row = "search:orbit"))), actions)
    }

    @Test
    fun emptyState_keepsQueryAndOffersClearRecovery() {
        val actions = mutableListOf<SearchAction>()
        setContent(
            state = SearchPreviewData.empty,
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(SearchTestTags.Empty).assertIsDisplayed()
        composeRule.onNodeWithTag(SearchTestTags.Field).assertTextEquals("unknown title")
        composeRule.onNodeWithText("Clear search").performClick()

        assertEquals(listOf(SearchAction.ClearQuery), actions)
    }

    private fun setContent(
        state: com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState,
        onAction: (SearchAction) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileSearchScreen(
                    state = state,
                    onAction = onAction,
                    gridState = rememberLazyGridState(),
                )
            }
        }
    }
}
