package com.pampoukidis.streamcoretv.feature.library.mobile.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryPreviewData
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MobileLibraryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyLibraryKeepsAllGuidanceSectionsVisible() {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileLibraryScreen(
                    state = LibraryUiState(isLoading = false),
                    onAction = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithTag(LibraryTestTags.ContinueWatching).assertExists()
        composeRule.onNodeWithTag(LibraryTestTags.Liked).assertExists()
        composeRule.onNodeWithTag(LibraryTestTags.MyList).assertExists()
        composeRule.onNodeWithTag(
            LibraryTestTags.EmptyPrefix + LibraryTestTags.ContinueWatching,
        ).assertExists()
        composeRule.onNodeWithTag(
            LibraryTestTags.EmptyPrefix + LibraryTestTags.Liked,
        ).assertExists()
        composeRule.onNodeWithTag(
            LibraryTestTags.EmptyPrefix + LibraryTestTags.MyList,
        ).assertExists()
    }

    @Test
    fun savedContentClickDispatchesSelection() {
        val actions = mutableListOf<LibraryAction>()
        val content = LibraryPreviewData.contentState.likedContent.first()
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileLibraryScreen(
                    state = LibraryPreviewData.contentState,
                    onAction = actions::add,
                    onProfileSelected = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(LibraryTestTags.ContentPrefix + content.row + ":" + content.id)
            .performClick()

        assertTrue(actions.last() is LibraryAction.ContentSelected)
    }

    @Test
    fun loadingUsesShelfPlaceholders() {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = false) {
                MobileLibraryScreen(
                    state = LibraryUiState(isLoading = true),
                    onAction = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithTag(LibraryTestTags.Loading).assertExists()
    }
}
