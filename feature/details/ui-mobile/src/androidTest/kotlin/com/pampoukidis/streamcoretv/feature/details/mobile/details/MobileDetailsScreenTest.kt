package com.pampoukidis.streamcoretv.feature.details.mobile.details

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MobileDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentStateRendersCinematicHierarchy() {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileDetailsScreen(
                    state = contentState(),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(DetailsTestTags.Hero).assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.Overview).assertExists()
        composeRule.onNodeWithText(DetailsPreviewData.content.title).assertExists()
        composeRule.onNodeWithText(DetailsPreviewData.content.description).assertExists()
    }

    @Test
    fun heroControlsDispatchBackAndRefreshActions() {
        val actions = mutableListOf<DetailsAction>()

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileDetailsScreen(
                    state = contentState(),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag(DetailsTestTags.BackButton).performClick()
        composeRule.onNodeWithTag(DetailsTestTags.RefreshButton).performClick()

        assertTrue(actions.contains(DetailsAction.BackSelected))
        assertTrue(actions.contains(DetailsAction.Refresh))
    }

    @Test
    fun unavailableStateProvidesBackAndRetryActions() {
        val actions = mutableListOf<DetailsAction>()

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileDetailsScreen(
                    state = DetailsUiState(isLoading = false),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithTag(DetailsTestTags.Error).assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.BackButton).performClick()
        composeRule.onNodeWithTag(DetailsTestTags.RetryButton).performClick()

        assertTrue(actions.contains(DetailsAction.BackSelected))
        assertTrue(actions.contains(DetailsAction.Refresh))
    }

    private fun contentState(): DetailsUiState {
        return DetailsUiState(
            isLoading = false,
            content = DetailsPreviewData.content.copy(
                poster = "",
                backdrop = null,
            ),
            recommendations = DetailsPreviewData.recommendations.map { recommendation ->
                recommendation.copy(
                    poster = "",
                    backdrop = null,
                )
            },
        )
    }
}
