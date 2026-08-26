package com.pampoukidis.streamcoretv.feature.details.tv.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TvDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playReceivesInitialFocusAndDispatchesExactlyOnce() {
        val actions = mutableListOf<DetailsAction>()

        setContent(stateProvider = ::contentState, actions = actions)

        composeRule.onNodeWithText("Play").assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.PlayButton)
            .assertIsFocused()
            .press(Key.Enter)

        assertEquals(listOf(DetailsAction.PlaySelected), actions)
    }

    @Test
    fun resumableContentUsesResumeLabel() {
        setContent(
            stateProvider = { contentState().copy(hasResumableProgress = true) },
        )

        composeRule.onNodeWithText("Resume").assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.PlayButton).assertIsFocused()
    }

    @Test
    fun dpadTraversalMovesPlayLikeMyListAndFirstRecommendation() {
        setContent(stateProvider = ::contentState)

        composeRule.onNodeWithTag(DetailsTestTags.PlayButton)
            .assertIsFocused()
            .press(Key.DirectionDown)
        composeRule.onNodeWithTag(DetailsTestTags.LikeAction)
            .assertIsFocused()
            .press(Key.DirectionRight)
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction)
            .assertIsFocused()
            .press(Key.DirectionDown)
        composeRule.onNodeWithTag(
            DetailsTestTags.RecommendationPrefix + DetailsPreviewData.recommendations.first().id,
        ).assertIsFocused()
    }

    @Test
    fun backAndRefreshRemainReachableFromPrimaryAction() {
        setContent(stateProvider = ::contentState)

        composeRule.onNodeWithTag(DetailsTestTags.PlayButton).press(Key.DirectionUp)
        composeRule.onNodeWithTag(DetailsTestTags.BackButton)
            .assertIsFocused()
            .press(Key.DirectionRight)
        composeRule.onNodeWithTag(DetailsTestTags.RefreshButton).assertIsFocused()
    }

    @Test
    fun selectedSavedActionsRenderSemanticsAndDispatchIndependently() {
        val actions = mutableListOf<DetailsAction>()

        setContent(
            stateProvider = {
                contentState().copy(
                    isLiked = true,
                    isInMyList = false,
                )
            },
            actions = actions,
        )

        composeRule.onNodeWithTag(DetailsTestTags.LikeAction).assertIsSelected()
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction).assertIsNotSelected()
        composeRule.onNodeWithTag(DetailsTestTags.PlayButton).press(Key.DirectionDown)
        composeRule.onNodeWithTag(DetailsTestTags.LikeAction)
            .assertIsFocused()
            .press(Key.Enter)
        composeRule.onNodeWithTag(DetailsTestTags.LikeAction).press(Key.DirectionRight)
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction)
            .assertIsFocused()
            .press(Key.Enter)

        assertEquals(
            listOf(DetailsAction.LikeToggled, DetailsAction.MyListToggled),
            actions,
        )
    }

    @Test
    fun pendingMutationKeepsFocusAndLeavesOtherActionEnabled() {
        val actions = mutableListOf<DetailsAction>()
        var state by mutableStateOf(contentState())

        setContent(stateProvider = { state }, actions = actions)
        composeRule.onNodeWithTag(DetailsTestTags.PlayButton).press(Key.DirectionDown)
        composeRule.onNodeWithTag(DetailsTestTags.LikeAction).assertIsFocused()

        state = state.copy(isLikeMutationPending = true)

        composeRule.onNodeWithTag(DetailsTestTags.LikeAction)
            .assertIsFocused()
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Updating",
                ),
            )
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction).assertIsEnabled()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun unavailableActionsAreDisabledAndSkippedDuringTraversal() {
        val actions = mutableListOf<DetailsAction>()

        setContent(
            stateProvider = { contentState().copy(isLibraryAvailable = false) },
            actions = actions,
        )

        composeRule.onNodeWithTag(DetailsTestTags.LikeAction).assertIsNotEnabled()
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction).assertIsNotEnabled()
        composeRule.onNodeWithTag(DetailsTestTags.PlayButton).press(Key.DirectionDown)
        composeRule.onNodeWithTag(
            DetailsTestTags.RecommendationPrefix + DetailsPreviewData.recommendations.first().id,
        ).assertIsFocused()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun retainedContentLoadingPreservesActionsAndRegressionContent() {
        setContent(
            stateProvider = {
                contentState().copy(
                    isLoading = true,
                    isMyListMutationPending = true,
                )
            },
        )

        composeRule.onNodeWithTag(DetailsTestTags.PlayButton).assertIsEnabled()
        composeRule.onNodeWithTag(DetailsTestTags.LikeAction).assertIsEnabled()
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction).assertIsNotEnabled()
        composeRule.onNodeWithTag(DetailsTestTags.Hero).assertExists()
        composeRule.onNodeWithText(DetailsPreviewData.content.description).assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.Recommendations).assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.RefreshButton).assertIsNotEnabled()
        composeRule.onNodeWithTag(DetailsTestTags.BackButton).assertIsEnabled()
    }

    private fun setContent(
        stateProvider: () -> DetailsUiState,
        actions: MutableList<DetailsAction> = mutableListOf(),
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvDetailsScreen(
                    state = stateProvider(),
                    onAction = actions::add,
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(DetailsEntranceSettleMillis)
        composeRule.waitUntil(timeoutMillis = DetailsEntranceTimeoutMillis) {
            composeRule.onAllNodesWithTag(DetailsTestTags.PlayButton)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
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
            isLibraryAvailable = true,
        )
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.press(key: Key) {
        performKeyInput {
            keyDown(key)
            keyUp(key)
        }
    }

    private companion object {
        const val DetailsEntranceSettleMillis = 500L
        const val DetailsEntranceTimeoutMillis = 5_000L
    }
}
