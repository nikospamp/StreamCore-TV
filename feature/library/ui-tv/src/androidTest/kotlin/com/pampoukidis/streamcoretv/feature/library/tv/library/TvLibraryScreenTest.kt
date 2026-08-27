package com.pampoukidis.streamcoretv.feature.library.tv.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryPreviewData
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvLibraryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstContentReceivesInitialFocusAndDispatchesSelection() {
        val actions = mutableListOf<LibraryAction>()
        val content = LibraryPreviewData.contentState.continueWatching.first()
        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(LibraryTestTags.ContentPrefix + content.row + ":" + content.id)
            .assertIsFocused()
            .performClick()

        assertEquals(listOf(LibraryAction.ContentSelected(content)), actions)
    }

    @Test
    fun emptyLibraryFocusesRefresh() {
        setScreen(state = LibraryUiState(isLoading = false))

        composeRule.onNodeWithTag(LibraryTestTags.Refresh).assertIsFocused()
        composeRule
            .onNodeWithTag(LibraryTestTags.EmptyPrefix + LibraryTestTags.MyList)
            .assertIsDisplayed()
    }

    @Test
    fun retryDispatchesAction() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(
            state = LibraryUiState(
                isLoading = false,
                error = com.pampoukidis.streamcoretv.core.model.error.AppError.Network(),
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(LibraryTestTags.Retry).performClick()

        assertEquals(listOf(LibraryAction.Retry), actions)
    }

    private fun setScreen(
        state: LibraryUiState = LibraryPreviewData.contentState,
        onAction: (LibraryAction) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvLibraryScreen(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }
}
