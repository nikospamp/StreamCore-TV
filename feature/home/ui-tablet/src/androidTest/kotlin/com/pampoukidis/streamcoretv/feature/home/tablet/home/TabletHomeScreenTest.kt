package com.pampoukidis.streamcoretv.feature.home.tablet.home

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TabletHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chromeHeroAndContinueWatchingExposeExpectedBehavior() {
        val actions = mutableListOf<HomeAction>()
        var profileSelected = false

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TabletHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = testRows(),
                    ),
                    onAction = actions::add,
                    onProfileSelected = { profileSelected = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Search").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Choose profile").performClick()
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails).performClick()
        composeRule
            .onNodeWithTag(HomeTestTags.RowPrefix + "continue-watching")
            .assertExists()

        assertTrue(profileSelected)
        assertTrue(actions.last() is HomeAction.ContentSelected)
    }

    @Test
    fun heroExpandsWhenContinueWatchingIsAbsent() {
        val rows = testRows().filterNot { row ->
            row.type == RowType.ContinueWatching
        }

        composeRule.setContent {
            StreamCoreTheme {
                TabletHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = rows,
                    ),
                    onAction = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(HomeTestTags.RowPrefix + "continue-watching")
            .assertDoesNotExist()
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails).assertExists()
    }

    private fun testRows(): List<RowModel> {
        return HomePreviewData.rows.map { row ->
            row.copy(
                content = row.content.map { content ->
                    content.copy(
                        poster = "",
                        backdrop = null,
                    )
                },
            )
        }
    }
}
