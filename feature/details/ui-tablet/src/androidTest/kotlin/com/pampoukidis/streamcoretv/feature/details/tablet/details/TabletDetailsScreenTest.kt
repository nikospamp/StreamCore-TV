package com.pampoukidis.streamcoretv.feature.details.tablet.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TabletDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playAndResumeLabelsDispatchExactlyOnePlayAction() {
        val actions = mutableListOf<DetailsAction>()
        var state by mutableStateOf(contentState())

        setContent(stateProvider = { state }, actions = actions)

        composeRule.onNodeWithText("Play").assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.PlayButton).performClick()
        assertEquals(listOf(DetailsAction.PlaySelected), actions)

        state = state.copy(hasResumableProgress = true)
        composeRule.onNodeWithText("Resume").assertExists()
        actions.clear()
        composeRule.onNodeWithTag(DetailsTestTags.PlayButton).performClick()
        assertEquals(listOf(DetailsAction.PlaySelected), actions)
    }

    @Test
    fun trailerActionRendersOnlyWhenAvailableAndDispatchesExactlyOnce() {
        val actions = mutableListOf<DetailsAction>()
        var state by mutableStateOf(contentState())

        setContent(stateProvider = { state }, actions = actions)

        composeRule.onNodeWithTag(DetailsTestTags.TrailerAction).assertDoesNotExist()

        state = state.copy(
            content = state.content?.copy(
                trailers = listOf(
                    TrailerModel(
                        id = "official-trailer",
                        title = "Official trailer",
                        url = "https://example.test/trailer",
                    ),
                ),
            ),
        )
        composeRule.onNodeWithTag(DetailsTestTags.TrailerAction).performClick()

        assertEquals(listOf(DetailsAction.TrailerSelected), actions)
    }

    @Test
    fun savedActionsRenderSelectionAndDispatchIndependently() {
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

        composeRule.onNodeWithTag(DetailsTestTags.LikeAction).assertIsSelected().performClick()
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction).assertIsNotSelected().performClick()

        assertEquals(
            listOf(DetailsAction.LikeToggled, DetailsAction.MyListToggled),
            actions,
        )
    }

    @Test
    fun pendingMutationDisablesOnlyItsOwnActionAndAnnouncesUpdating() {
        val actions = mutableListOf<DetailsAction>()

        setContent(
            stateProvider = { contentState().copy(isLikeMutationPending = true) },
            actions = actions,
        )

        composeRule.onNodeWithTag(DetailsTestTags.LikeAction)
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Updating",
                ),
            )
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction)
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf(DetailsAction.MyListToggled), actions)
    }

    @Test
    fun unavailableLibraryActionsAreDisabledAndDoNotDispatch() {
        val actions = mutableListOf<DetailsAction>()

        setContent(
            stateProvider = { contentState().copy(isLibraryAvailable = false) },
            actions = actions,
        )

        composeRule.onNodeWithTag(DetailsTestTags.LikeAction).assertIsNotEnabled()
        composeRule.onNodeWithTag(DetailsTestTags.MyListAction).assertIsNotEnabled()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun retainedContentLoadingKeepsActionsAndDisablesOnlyRefresh() {
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
        composeRule.onNodeWithTag(DetailsTestTags.RefreshButton).assertIsNotEnabled()
        composeRule.onNodeWithTag(DetailsTestTags.BackButton).assertIsEnabled()
    }

    @Test
    fun metadataCastAndRecommendationsRemainRendered() {
        setContent(stateProvider = ::contentState)

        composeRule.onNodeWithTag(DetailsTestTags.Hero).assertExists()
        composeRule.onNodeWithText(DetailsPreviewData.content.title).assertExists()
        composeRule.onNodeWithText(DetailsPreviewData.content.description).assertExists()
        composeRule.onNodeWithText("Alex Morgan as Commander Vale · Jordan Lee as Dr. Ilya Chen")
            .assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.Recommendations)
            .performScrollTo()
            .assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.RefreshButton).assertExists()
        composeRule.onNodeWithTag(DetailsTestTags.BackButton).assertExists()
    }

    private fun setContent(
        stateProvider: () -> DetailsUiState,
        actions: MutableList<DetailsAction> = mutableListOf(),
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TabletDetailsScreen(
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

    private companion object {
        const val DetailsEntranceSettleMillis = 500L
        const val DetailsEntranceTimeoutMillis = 5_000L
    }
}
