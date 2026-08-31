package com.pampoukidis.streamcoretv.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.test.espresso.Espresso.pressBack
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags
import com.pampoukidis.streamcoretv.feature.home.tv.home.TvHomeScreen
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryPreviewData
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import com.pampoukidis.streamcoretv.feature.library.tv.library.TvLibraryScreen
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchPreviewData
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import com.pampoukidis.streamcoretv.feature.search.tv.search.TvSearchScreen
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.tv.profiles.TvProfilesScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TvNavigationDrawerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun leftFromContentExpandsDrawerAndFocusesSelectedDestination() {
        assertDirectDrawerEntry(TopLevelDestination.Home)
    }

    @Test
    fun searchReceivesFocusDirectlyOnEntryAndReentry() {
        assertDirectDrawerEntry(TopLevelDestination.Search)
    }

    @Test
    fun libraryReceivesFocusDirectlyOnEntryAndReentry() {
        assertDirectDrawerEntry(TopLevelDestination.Library)
    }

    @Test
    fun rightFromDrawerRestoresExactHomeCard() {
        assertHomeCardRestored {
            composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Home))
                .performKeyInput { pressKey(Key.DirectionRight) }
        }
    }

    @Test
    fun backFromDrawerRestoresExactHomeCard() {
        assertHomeCardRestored { pressBack() }
    }

    @Test
    fun selectingCurrentDestinationRestoresExactHomeCard() {
        assertHomeCardRestored {
            composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Home))
                .performKeyInput { pressKey(Key.Enter) }
        }
    }

    @Test
    fun rightFromDrawerRestoresLibrarySectionAndCard() {
        val state = LibraryPreviewData.contentState
        val card = state.likedContent.first()
        val cardTag = LibraryTestTags.ContentPrefix + card.row + ":" + card.id
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvNavigationDrawer(
                    enabled = true,
                    selectedDestination = TopLevelDestination.Library,
                    activeProfile = null,
                    onDestinationSelected = {},
                    onProfileSelected = {},
                ) {
                    TvLibraryScreen(
                        state = state,
                        onAction = {},
                        returnFocusKey = StreamCoreSharedKey.content(card.id, card.row),
                    )
                }
            }
        }
        composeRule.onNodeWithTag(cardTag).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Library))
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag(cardTag).assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun drawerBackRestoresSearchResultBeforeScreenBackHandler() {
        val items = SearchPreviewData.items.map {
            it.copy(row = "search:orbit", poster = "", backdrop = null)
        }
        val card = items.first()
        var screenBackCalls = 0
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvNavigationDrawer(
                    enabled = true,
                    selectedDestination = TopLevelDestination.Search,
                    activeProfile = null,
                    onDestinationSelected = {},
                    onProfileSelected = {},
                ) {
                    BackHandler { screenBackCalls += 1 }
                    TvSearchScreen(
                        state = SearchPreviewData.results.copy(content = SearchContentState.Results(items)),
                        onAction = {},
                        onKeyboardRequested = {},
                        fieldFocusRequester = remember { FocusRequester() },
                        returnFocusKey = StreamCoreSharedKey.content(card.id, card.row),
                    )
                }
            }
        }
        composeRule.onNodeWithTag(SearchTestTags.result(card.id)).assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Search))
            .assertIsFocused()

        pressBack()

        composeRule.onNodeWithTag(SearchTestTags.result(card.id)).assertIsFocused().assertIsDisplayed()
        assertEquals(0, screenBackCalls)
        pressBack()
        assertEquals(1, screenBackCalls)
    }

    @Test
    fun detailsBackKeepsDrawerClosedAndRestoresSearchResult() {
        val items = SearchPreviewData.items.map { content ->
            content.copy(row = "search:orbit", poster = "", backdrop = null)
        }
        val origin = items.first()
        val originKey = StreamCoreSharedKey.content(origin.id, origin.row)
        var showDetails by mutableStateOf(false)
        var returnFocusKey by mutableStateOf<String?>(originKey)

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvNavigationDrawer(
                    enabled = !showDetails,
                    selectedDestination = TopLevelDestination.Search,
                    activeProfile = null,
                    onDestinationSelected = {},
                    onProfileSelected = {},
                ) {
                    AnimatedContent(targetState = showDetails, label = "details-return") { details ->
                        if (details) {
                            val backFocusRequester = remember { FocusRequester() }
                            LaunchedEffect(backFocusRequester) {
                                withFrameNanos { }
                                backFocusRequester.requestFocus()
                            }
                            BackHandler {
                                returnFocusKey = originKey
                                showDetails = false
                            }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                StreamCoreTvButton(
                                    text = "Back",
                                    onClick = {
                                        returnFocusKey = originKey
                                        showDetails = false
                                    },
                                    enabled = true,
                                    modifier = Modifier
                                        .focusRequester(backFocusRequester)
                                        .testTag(DetailsBackTag),
                                )
                            }
                        } else {
                            TvSearchScreen(
                                state = SearchPreviewData.results.copy(
                                    content = SearchContentState.Results(items),
                                ),
                                onAction = { action ->
                                    if (action is com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction.ResultSelected) {
                                        showDetails = true
                                    }
                                },
                                onKeyboardRequested = {},
                                fieldFocusRequester = remember { FocusRequester() },
                                returnFocusKey = returnFocusKey,
                                onReturnFocusConsumed = { consumedKey ->
                                    if (returnFocusKey == consumedKey) {
                                        returnFocusKey = null
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag(SearchTestTags.result(origin.id)).assertIsFocused().performClick()
        composeRule.onNodeWithTag(DetailsBackTag).assertIsFocused()
        pressBack()

        composeRule.onNodeWithTag(SearchTestTags.result(origin.id)).assertIsFocused()
        composeRule.onNodeWithTag(TvNavigationTestTags.Drawer)
            .assertWidthIsEqualTo(
                StreamCoreDimens.Tv.Navigation.CollapsedWidth -
                        StreamCoreDimens.Spacing.Small * 2,
            )
        composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Search))
            .assertIsNotFocused()
    }

    @Test
    fun destinationAndProfileClicksDispatchCallbacks() {
        val destinations = mutableListOf<TopLevelDestination>()
        var profileClicks = 0
        setDrawer(
            onDestinationSelected = destinations::add,
            onProfileSelected = { profileClicks += 1 },
        )

        composeRule
            .onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Search))
            .performClick()
        composeRule.onNodeWithTag(TvNavigationTestTags.Profile).performClick()

        assertEquals(listOf(TopLevelDestination.Search), destinations)
        assertEquals(1, profileClicks)
    }

    @Test
    fun avatarKeepsSquareSizeAndPositionWhenDrawerExpandsAndCollapses() {
        val profile = ProfilesPreviewData.profiles.first()
        setDrawer(activeProfile = profile)
        val avatar = composeRule.onNodeWithTag(TvNavigationTestTags.Avatar, useUnmergedTree = true)

        avatar.assertWidthIsEqualTo(StreamCoreDimens.Icon.Large)
            .assertHeightIsEqualTo(StreamCoreDimens.Icon.Large)
        val collapsedLeft = avatar.getUnclippedBoundsInRoot().left
        composeRule.onNodeWithText(profile.displayName).assertDoesNotExist()

        composeRule.onNodeWithTag(ContentTag).performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Home))
            .assertIsFocused()
        avatar.assertWidthIsEqualTo(StreamCoreDimens.Icon.Large)
            .assertHeightIsEqualTo(StreamCoreDimens.Icon.Large)
        assertEquals(collapsedLeft, avatar.getUnclippedBoundsInRoot().left)
        composeRule.onNodeWithText(profile.displayName).assertIsDisplayed()

        composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Home))
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag(ContentTag).assertIsFocused()
        avatar.assertWidthIsEqualTo(StreamCoreDimens.Icon.Large)
            .assertHeightIsEqualTo(StreamCoreDimens.Icon.Large)
        assertEquals(collapsedLeft, avatar.getUnclippedBoundsInRoot().left)
        composeRule.onNodeWithText(profile.displayName).assertDoesNotExist()
    }

    @Test
    fun selectedProfileAvatarTransitionsToDrawerAndBack() {
        val profile = ProfilesPreviewData.profiles.last()
        var showHome by mutableStateOf(false)
        lateinit var transitionScope: SharedTransitionScope

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                    transitionScope = this
                    TvNavigationDrawer(
                        enabled = showHome,
                        selectedDestination = TopLevelDestination.Home,
                        activeProfile = profile,
                        onDestinationSelected = {},
                        onProfileSelected = { showHome = false },
                        sharedTransitionScope = this,
                    ) {
                        AnimatedContent(targetState = showHome, label = "profile-navigation") { home ->
                            if (home) {
                                val focusRequester = remember { FocusRequester() }
                                LaunchedEffect(focusRequester) {
                                    withFrameNanos { }
                                    focusRequester.requestFocus()
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    StreamCoreTvButton(
                                        text = "Content",
                                        onClick = {},
                                        enabled = true,
                                        modifier = Modifier
                                            .focusRequester(focusRequester)
                                            .testTag(ContentTag),
                                    )
                                }
                            } else {
                                TvProfilesScreen(
                                    state = ProfilesUiState(
                                        isLoading = false,
                                        profiles = ProfilesPreviewData.profiles,
                                    ),
                                    onAction = { action ->
                                        if (action == ProfilesAction.SelectProfile(profile.id)) {
                                            showHome = true
                                        }
                                    },
                                    onCreateProfile = {},
                                    onEditProfile = {},
                                    sharedElementScope = StreamCoreSharedElementScope(
                                        sharedTransitionScope = transitionScope,
                                        animatedVisibilityScope = this,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + profile.id).performClick()
        composeRule.mainClock.advanceTimeBy(64)
        composeRule.runOnIdle {
            assertTrue("Selected avatar must match the drawer avatar", transitionScope.isTransitionActive)
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.runOnIdle { assertFalse(transitionScope.isTransitionActive) }
        composeRule.onNodeWithTag(ContentTag).performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Home))
            .assertIsFocused()
            .performKeyInput {
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
                pressKey(Key.DirectionDown)
            }
        composeRule.onNodeWithTag(TvNavigationTestTags.Profile)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        composeRule.mainClock.advanceTimeBy(64)
        composeRule.runOnIdle {
            assertTrue("Drawer avatar must survive the return transition", transitionScope.isTransitionActive)
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.runOnIdle { assertFalse(transitionScope.isTransitionActive) }
        composeRule.onNodeWithTag(TvNavigationTestTags.Drawer).assertDoesNotExist()
        composeRule.onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + profile.id).assertIsDisplayed()
    }

    private fun assertHomeCardRestored(closeDrawer: () -> Unit) {
        val rows = HomePreviewData.rows.filter { it.type != RowType.Featured }.map { row ->
            row.copy(content = row.content.map { it.copy(poster = "", backdrop = null) })
        }
        val row = rows.first { it.type == RowType.TopTen }
        val card = row.content.first()
        val cardTag = HomeTestTags.ContentCardPrefix + row.id + ":" + card.id

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvNavigationDrawer(
                    enabled = true,
                    selectedDestination = TopLevelDestination.Home,
                    activeProfile = ProfilesPreviewData.profiles.first(),
                    onDestinationSelected = {},
                    onProfileSelected = {},
                ) {
                    TvHomeScreen(
                        state = HomeUiState(isLoading = false, rows = rows),
                        onAction = {},
                        returnFocusKey = StreamCoreSharedKey.content(card.id, card.row),
                    )
                }
            }
        }

        repeat(3) {
            composeRule.onNodeWithTag(cardTag)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionLeft) }
            composeRule.onNodeWithTag(TvNavigationTestTags.destination(TopLevelDestination.Home))
                .assertIsFocused()

            closeDrawer()

            composeRule.onNodeWithTag(cardTag).assertIsFocused().assertIsDisplayed()
        }
        composeRule.onNodeWithTag(cardTag).performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag(HomeTestTags.ContentCardPrefix + row.id + ":" + row.content[1].id)
            .assertIsFocused()
    }

    private fun assertDirectDrawerEntry(selectedDestination: TopLevelDestination) {
        lateinit var focusManager: FocusManager
        setDrawer(
            selectedDestination = selectedDestination,
            onFocusManagerAvailable = { focusManager = it },
        )
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        // Key injection can advance the test clock. Run its focus search synchronously so
        // this assertion catches even a one-frame focus correction after entry.
        composeRule.runOnIdle { assertTrue(focusManager.moveFocus(FocusDirection.Left)) }
        composeRule.onNodeWithTag(TvNavigationTestTags.destination(selectedDestination))
            .assertIsFocused()
        composeRule.mainClock.advanceTimeBy(500)

        val adjacentDestination = if (selectedDestination == TopLevelDestination.Search) {
            TopLevelDestination.Library
        } else {
            TopLevelDestination.Search
        }
        val direction = if (selectedDestination == TopLevelDestination.Library) {
            FocusDirection.Up
        } else {
            FocusDirection.Down
        }
        composeRule.runOnIdle { assertTrue(focusManager.moveFocus(direction)) }
        composeRule.onNodeWithTag(TvNavigationTestTags.destination(adjacentDestination))
            .assertIsFocused()

        composeRule.runOnIdle { assertTrue(focusManager.moveFocus(FocusDirection.Right)) }
        composeRule.onNodeWithTag(ContentTag).assertIsFocused()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.runOnIdle { assertTrue(focusManager.moveFocus(FocusDirection.Left)) }
        composeRule.onNodeWithTag(TvNavigationTestTags.destination(selectedDestination))
            .assertIsFocused()
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText(selectedDestination.name).assertIsDisplayed()
    }

    private fun setDrawer(
        onDestinationSelected: (TopLevelDestination) -> Unit = {},
        onProfileSelected: () -> Unit = {},
        activeProfile: ProfileModel? = null,
        selectedDestination: TopLevelDestination = TopLevelDestination.Home,
        onFocusManagerAvailable: (FocusManager) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                onFocusManagerAvailable(LocalFocusManager.current)
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(focusRequester) {
                    withFrameNanos { }
                    focusRequester.requestFocus()
                }
                TvNavigationDrawer(
                    enabled = true,
                    selectedDestination = selectedDestination,
                    activeProfile = activeProfile,
                    onDestinationSelected = onDestinationSelected,
                    onProfileSelected = onProfileSelected,
                ) {
                    Box(
                        // Keep the focus target outside the expanded drawer's bounds.
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = StreamCoreDimens.Tv.Navigation.ContentStartPadding),
                    ) {
                        StreamCoreTvButton(
                            text = "Content",
                            onClick = {},
                            enabled = true,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .testTag(ContentTag),
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val ContentTag = "tv-navigation:content"
        const val DetailsBackTag = "tv-navigation:details-back"
    }
}
