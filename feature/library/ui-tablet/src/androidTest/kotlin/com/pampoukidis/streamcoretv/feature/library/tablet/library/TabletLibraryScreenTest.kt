package com.pampoukidis.streamcoretv.feature.library.tablet.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryPreviewData
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TabletLibraryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyLibraryKeepsAllGuidanceSectionsVisible() {
        setScreen(state = LibraryUiState(isLoading = false))

        composeRule.onNodeWithTag(LibraryTestTags.ContinueWatching).assertIsDisplayed()
        composeRule.onNodeWithTag(
            LibraryTestTags.EmptyPrefix + LibraryTestTags.ContinueWatching,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(LibraryTestTags.Root).performScrollToNode(
            hasTestTag(LibraryTestTags.Liked),
        )
        composeRule.onNodeWithTag(LibraryTestTags.Liked).assertIsDisplayed()
        composeRule.onNodeWithTag(
            LibraryTestTags.EmptyPrefix + LibraryTestTags.Liked,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(LibraryTestTags.Root).performScrollToNode(
            hasTestTag(LibraryTestTags.MyList),
        )
        composeRule.onNodeWithTag(LibraryTestTags.MyList).assertIsDisplayed()
        composeRule.onNodeWithTag(
            LibraryTestTags.EmptyPrefix + LibraryTestTags.MyList,
        ).assertIsDisplayed()
    }

    @Test
    fun contentAndProfileClicksDispatchCallbacks() {
        val actions = mutableListOf<LibraryAction>()
        var profileClicks = 0
        val content = LibraryPreviewData.contentState.likedContent.first()
        setScreen(
            state = LibraryPreviewData.contentState,
            onAction = actions::add,
            onProfileSelected = { profileClicks++ },
        )

        val contentTag = LibraryTestTags.ContentPrefix + content.row + ":" + content.id
        composeRule.onNodeWithTag(LibraryTestTags.Root).performScrollToNode(
            hasTestTag(contentTag),
        )
        composeRule.onNodeWithTag(contentTag).performClick()
        composeRule.onNodeWithTag(LibraryTestTags.Profile).performClick()

        assertEquals(listOf(LibraryAction.ContentSelected(content)), actions)
        assertEquals(1, profileClicks)
    }

    @Test
    fun loadingAndFailureExposeProgressAndRetry() {
        val actions = mutableListOf<LibraryAction>()
        var state by mutableStateOf(LibraryUiState(isLoading = true))
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TabletLibraryScreen(
                    state = state,
                    onAction = actions::add,
                    onProfileSelected = {},
                )
            }
        }
        composeRule.onNodeWithTag(LibraryTestTags.Loading).assertIsDisplayed()

        composeRule.runOnIdle {
            state = LibraryUiState(
                isLoading = false,
                error = AppError.Network(),
            )
        }
        composeRule.onNodeWithTag(LibraryTestTags.Error).assertIsDisplayed()
        composeRule.onNodeWithTag(LibraryTestTags.Retry).performClick()

        assertEquals(listOf(LibraryAction.Retry), actions)
    }

    @Test
    fun libraryMutationRemovesStaleContent() {
        var state by mutableStateOf(LibraryPreviewData.contentState)
        val content = state.myListContent.first()
        val tag = LibraryTestTags.ContentPrefix + content.row + ":" + content.id
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TabletLibraryScreen(
                    state = state,
                    onAction = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithTag(LibraryTestTags.Root).performScrollToNode(hasTestTag(tag))
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
        composeRule.runOnIdle {
            state = state.copy(
                myListContent = state.myListContent.filterNot { item -> item.id == content.id },
            )
        }
        composeRule.onNodeWithTag(tag).assertDoesNotExist()
    }

    private fun setScreen(
        state: LibraryUiState,
        onAction: (LibraryAction) -> Unit = {},
        onProfileSelected: () -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TabletLibraryScreen(
                    state = state,
                    onAction = onAction,
                    onProfileSelected = onProfileSelected,
                )
            }
        }
    }
}
