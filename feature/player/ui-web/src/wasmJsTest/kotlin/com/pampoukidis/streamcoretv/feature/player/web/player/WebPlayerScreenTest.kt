package com.pampoukidis.streamcoretv.feature.player.web.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerViewModel
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.feature.player.web.testing.FakeWebPlaybackSession
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerFixtures
import com.pampoukidis.streamcoretv.feature.player.web.testing.WebPlayerShowcaseScenario
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebPlayerScreenTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun routeLoadsRequestThroughFakeSessionAndForwardsBackEffect(): TestResult {
        val session = FakeWebPlaybackSession()
        val viewModel = PlayerViewModel(
            sourceRepository = FakeSourceRepository,
            progressRepository = FakeProgressRepository,
            sessionFactory = FixedSessionFactory(session),
        )
        var backCalls = 0
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebPlayerRoute(
                        request = WebPlayerFixtures.request,
                        onBack = { backCalls += 1 },
                        viewModel = viewModel,
                    )
                }
            }

            waitForIdle()
            assertEquals(1, session.prepareCalls)
            viewModel.onAction(PlayerAction.BackSelected)
            waitForIdle()
            assertEquals(1, backCalls)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun primaryControlsDispatchTypedPlaybackActions(): TestResult {
        val actions = mutableListOf<PlayerAction>()
        return runPlayerUiTest(
            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Playing),
            onAction = actions::add,
        ) {
            onNodeWithTag(PlayerTestTags.PlayPause)
                .assertIsFocused()
                .performSemanticsAction(SemanticsActions.OnClick)
            onNodeWithTag(PlayerTestTags.Rewind)
                .performSemanticsAction(SemanticsActions.OnClick)
            onNodeWithTag(PlayerTestTags.Forward)
                .performSemanticsAction(SemanticsActions.OnClick)
            onNodeWithTag(PlayerTestTags.SettingsButton)
                .performSemanticsAction(SemanticsActions.OnClick)

            assertTrue(
                actions.contains(
                    PlayerAction.SeekBy(
                        deltaMillis = -WebPlayerTokens.SeekIntervalMillis,
                        showFeedback = true,
                    ),
                ),
            )
            assertTrue(actions.contains(PlayerAction.TogglePlayPause))
            assertTrue(
                actions.contains(
                    PlayerAction.SeekBy(
                        deltaMillis = WebPlayerTokens.SeekIntervalMillis,
                        showFeedback = true,
                    ),
                ),
            )
            assertTrue(actions.contains(PlayerAction.OpenSettings()))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun arrowFocusGraphMovesAcrossControlsAndTimeline(): TestResult {
        return runPlayerUiTest(
            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Paused),
        ) {
            onNodeWithTag(PlayerTestTags.PlayPause)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }
            onNodeWithTag(PlayerTestTags.Forward)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithTag(PlayerTestTags.Timeline)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithTag(PlayerTestTags.SettingsButton).assertIsFocused()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun timelineArrowsScrubWithoutDispatchingGlobalSeek(): TestResult {
        val actions = mutableListOf<PlayerAction>()
        return runPlayerUiTest(
            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Paused),
            onAction = actions::add,
        ) {
            onNodeWithTag(PlayerTestTags.PlayPause)
                .performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithTag(PlayerTestTags.Timeline)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }

            val scrubActions = actions.takeLast(3)
            assertEquals(PlayerAction.ScrubStarted, scrubActions[0])
            assertIs<PlayerAction.ScrubChanged>(scrubActions[1])
            assertEquals(PlayerAction.ScrubFinished, scrubActions[2])
            assertFalse(actions.any { action -> action is PlayerAction.SeekBy })
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsBackDispatchesOneLayerReturnAction(): TestResult {
        val actions = mutableListOf<PlayerAction>()
        return runPlayerUiTest(
            state = WebPlayerFixtures
                .state(WebPlayerShowcaseScenario.Settings)
                .copy(settingsPage = PlayerSettingsPage.Quality),
            onAction = actions::add,
        ) {
            onNodeWithTag(
                testTag = PlayerTestTags.SettingsOptionPrefix + "automatic",
                useUnmergedTree = true,
            ).assertIsFocused()
            onNodeWithTag(WebPlayerTestTags.SettingsBack).performClick()
            assertEquals(listOf<PlayerAction>(PlayerAction.BackSelected), actions)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsSpaceActivatesFocusedSelectionOnly(): TestResult {
        val actions = mutableListOf<PlayerAction>()
        return runPlayerUiTest(
            state = WebPlayerFixtures
                .state(WebPlayerShowcaseScenario.Paused)
                .copy(
                    settingsPage = PlayerSettingsPage.Quality,
                    selectedVideoTrackId = null,
                ),
            onAction = actions::add,
        ) {
            onNodeWithTag(
                testTag = PlayerTestTags.SettingsOptionPrefix + "automatic",
                useUnmergedTree = true,
            )
                .assertIsFocused()
                .assertIsSelected()
                .performKeyInput { pressKey(Key.Spacebar) }

            assertEquals(listOf<PlayerAction>(PlayerAction.SelectVideoTrack(null)), actions)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun autoplayActivationIsExplicitAndPlayRemainsSemantic(): TestResult {
        val actions = mutableListOf<PlayerAction>()
        return runPlayerUiTest(
            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.AutoplayActivation),
            onAction = actions::add,
        ) {
            onNodeWithTag(
                testTag = WebPlayerTestTags.Activation,
                useUnmergedTree = true,
            ).assertIsDisplayed()
            onNodeWithTag(PlayerTestTags.PlayPause).performClick()
            assertEquals(
                1,
                actions.count { action -> action == PlayerAction.TogglePlayPause },
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun preparingBufferingAndEndedStatesRemainExplicit(): TestResult {
        var state by mutableStateOf(
            WebPlayerFixtures.state(WebPlayerShowcaseScenario.Preparing),
        )
        val actions = mutableListOf<PlayerAction>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebPlayerScreen(
                        state = state,
                        videoSurface = WebPlayerFixtures.videoSurface,
                        onAction = actions::add,
                    )
                }
            }

            onNodeWithTag(
                testTag = WebPlayerTestTags.Preparing,
                useUnmergedTree = true,
            ).assertIsDisplayed()
            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Buffering)
            waitForIdle()
            onNodeWithTag(
                testTag = PlayerTestTags.Buffering,
                useUnmergedTree = true,
            ).assertIsDisplayed()
            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Ended)
            waitForIdle()
            onNodeWithTag(
                testTag = WebPlayerTestTags.Ended,
                useUnmergedTree = true,
            ).assertIsDisplayed()
            onNodeWithTag(PlayerTestTags.PlayPause).performClick()
            assertEquals(PlayerAction.TogglePlayPause, actions.last())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun recoverableAndFatalErrorsExposeCorrectActions(): TestResult {
        var state by mutableStateOf(
            WebPlayerFixtures.state(WebPlayerShowcaseScenario.RecoverableError),
        )
        val actions = mutableListOf<PlayerAction>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebPlayerScreen(
                        state = state,
                        videoSurface = WebPlayerFixtures.videoSurface,
                        onAction = actions::add,
                    )
                }
            }

            onNodeWithTag(WebPlayerTestTags.ErrorRetry)
                .assertIsFocused()
                .performClick()
            assertEquals(PlayerAction.Retry, actions.last())

            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.FatalError)
            onAllNodesWithTag(WebPlayerTestTags.ErrorRetry).assertCountEquals(0)
            onNodeWithTag(WebPlayerTestTags.ErrorBack)
                .assertIsFocused()
                .performClick()
            assertEquals(PlayerAction.BackSelected, actions.last())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mouseMovementRevealsAutoHiddenControls(): TestResult {
        var state by mutableStateOf(
            WebPlayerFixtures.state(WebPlayerShowcaseScenario.ControlsHidden),
        )
        val actions = mutableListOf<PlayerAction>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebPlayerScreen(
                        state = state,
                        videoSurface = WebPlayerFixtures.videoSurface,
                        onAction = { action ->
                            actions += action
                            if (action == PlayerAction.UserInteraction) {
                                state = state.copy(controlsVisible = true)
                            }
                        },
                    )
                }
            }

            onNodeWithTag(PlayerTestTags.Root).assertIsFocused()
            onAllNodesWithTag(PlayerTestTags.PlayPause).assertCountEquals(0)
            actions.clear()
            onNodeWithTag(PlayerTestTags.Root).performMouseInput {
                moveTo(center)
            }
            onNodeWithTag(PlayerTestTags.PlayPause)
                .assertIsDisplayed()
                .assertIsFocused()
            assertTrue(actions.contains(PlayerAction.UserInteraction))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun keyboardInputRevealsAutoHiddenControls(): TestResult {
        var state by mutableStateOf(
            WebPlayerFixtures.state(WebPlayerShowcaseScenario.ControlsHidden),
        )
        val actions = mutableListOf<PlayerAction>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebPlayerScreen(
                        state = state,
                        videoSurface = WebPlayerFixtures.videoSurface,
                        onAction = { action ->
                            actions += action
                            if (action == PlayerAction.UserInteraction) {
                                state = state.copy(controlsVisible = true)
                            }
                        },
                    )
                }
            }

            onNodeWithTag(PlayerTestTags.Root)
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }
            onNodeWithTag(PlayerTestTags.PlayPause)
                .assertIsDisplayed()
                .assertIsFocused()
            assertTrue(actions.contains(PlayerAction.UserInteraction))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun noFilmstripFallbackKeepsScrubTimeAndTimelineAvailable(): TestResult {
        val state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Scrubbing)
        return runPlayerUiTest(
            state = state,
        ) {
            assertTrue(state.filmstripFrames.isNotEmpty())
            assertTrue(state.filmstripFrames.all { frame -> frame.image == null })
            onNodeWithTag(
                testTag = PlayerTestTags.Filmstrip,
                useUnmergedTree = true,
            ).assertIsDisplayed()
            onNodeWithTag(
                testTag = WebPlayerTestTags.NoFilmstrip,
                useUnmergedTree = true,
            ).fetchSemanticsNode()
            onNodeWithText("35:10", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag(PlayerTestTags.Timeline).assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fullscreenControlInvokesFullscreenCallback(): TestResult {
        val controller = RecordingFullscreenController()
        val actions = mutableListOf<PlayerAction>()
        return runComposeUiTest {
            setContent {
                CompositionLocalProvider(
                    LocalWebPlayerFullscreenController provides controller,
                ) {
                    StreamCoreTheme(darkTheme = true) {
                        WebPlayerScreen(
                            state = WebPlayerFixtures.state(WebPlayerShowcaseScenario.Paused),
                            videoSurface = WebPlayerFixtures.videoSurface,
                            onAction = actions::add,
                        )
                    }
                }
            }

            onNodeWithTag(WebPlayerTestTags.Fullscreen).performClick()
            assertTrue(controller.isFullscreen.value)
            assertEquals(1, controller.toggleCalls)
            assertEquals(0, controller.exitCalls)
            assertFalse(actions.contains(PlayerAction.BackSelected))

            onNodeWithTag(PlayerTestTags.Back).performClick()
            assertFalse(controller.isFullscreen.value)
            assertEquals(1, controller.toggleCalls)
            assertEquals(1, controller.exitCalls)
            assertFalse(actions.contains(PlayerAction.BackSelected))
        }
    }

    @Test
    fun fakeSessionRemainsBackendFreeAndRecordsCommands() {
        val session = FakeWebPlaybackSession(
            initialState = PlaybackEngineState(phase = PlaybackPhase.Ready),
        )

        session.play()
        session.seekTo(42_000L)
        session.setSpeed(1.5f)
        session.close()

        assertEquals(1, session.playCalls)
        assertEquals(42_000L, session.lastSeekPositionMillis)
        assertEquals(1.5f, session.lastSpeed)
        assertEquals(1, session.closeCalls)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun runPlayerUiTest(
        state: PlayerUiState,
        onAction: (PlayerAction) -> Unit = {},
        assertions: suspend androidx.compose.ui.test.ComposeUiTest.() -> Unit,
    ): TestResult {
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebPlayerScreen(
                        state = state,
                        videoSurface = WebPlayerFixtures.videoSurface,
                        onAction = onAction,
                    )
                }
            }
            assertions()
        }
    }

    private class RecordingFullscreenController : WebPlayerFullscreenController {
        private val mutableFullscreen = mutableStateOf(false)
        override val isFullscreen: State<Boolean> = mutableFullscreen

        var toggleCalls: Int = 0
            private set
        var exitCalls: Int = 0
            private set

        override fun toggle() {
            toggleCalls += 1
            mutableFullscreen.value = !mutableFullscreen.value
        }

        override fun exit() {
            exitCalls += 1
            mutableFullscreen.value = false
        }
    }

    private class FixedSessionFactory(
        private val session: PlaybackSession,
    ) : PlaybackSessionFactory {
        override fun create(): PlaybackSession {
            return session
        }
    }

    private object FakeSourceRepository : PlaybackSourceRepository {
        override suspend fun resolve(request: PlaybackRequestModel): PlaybackMediaModel {
            return PlaybackMediaModel(
                assetId = request.contentId,
                title = request.contentSnapshot.title,
            )
        }
    }

    private object FakeProgressRepository : PlaybackProgressRepository {
        override fun observe(profileId: String): Flow<List<PlaybackProgressEntryModel>> {
            return flowOf(emptyList())
        }

        override suspend fun get(
            profileId: String,
            contentId: String,
        ): PlaybackProgressEntryModel? {
            return null
        }

        override suspend fun upsert(entry: PlaybackProgressEntryModel) {
            return
        }

        override suspend fun remove(profileId: String, contentId: String) {
            return
        }
    }

    private companion object {
    }
}
