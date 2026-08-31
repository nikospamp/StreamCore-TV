package com.pampoukidis.streamcoretv.feature.player.tv.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.test.espresso.Espresso.pressBack
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TvPlayerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dpadControlsHaveDeterministicOrderAndDispatchSeek() {
        val actions = mutableListOf<PlayerAction>()
        setContent(stateProvider = ::readyState, actions = actions)

        composeRule.onNodeWithTag(PlayerTestTags.PlayPause)
            .assertIsFocused()
            .press(Key.DirectionLeft)
        composeRule.onNodeWithTag(PlayerTestTags.Rewind)
            .assertIsFocused()
            .press(Key.Enter)
        composeRule.onNodeWithTag(PlayerTestTags.Rewind).press(Key.DirectionRight)
        composeRule.onNodeWithTag(PlayerTestTags.PlayPause)
            .assertIsFocused()
            .press(Key.DirectionRight)
        composeRule.onNodeWithTag(PlayerTestTags.Forward)
            .assertIsFocused()
            .press(Key.Enter)
        composeRule.onNodeWithTag(PlayerTestTags.Forward).press(Key.DirectionDown)
        composeRule.onNodeWithTag(PlayerTestTags.Timeline)
            .assertIsFocused()
            .press(Key.DirectionDown)
        composeRule.onNodeWithTag(PlayerTestTags.SettingsButton).assertIsFocused()

        assertEquals(
            listOf(
                PlayerAction.SeekBy(-10_000L, showFeedback = true),
                PlayerAction.SeekBy(10_000L, showFeedback = true),
            ),
            actions.filterIsInstance<PlayerAction.SeekBy>(),
        )
    }

    @Test
    fun playPauseDispatchesExactlyOnce() {
        val actions = mutableListOf<PlayerAction>()
        setContent(stateProvider = ::readyState, actions = actions)

        composeRule.onNodeWithTag(PlayerTestTags.PlayPause).press(Key.Enter)

        assertEquals(
            listOf(PlayerAction.TogglePlayPause),
            actions.filter { it == PlayerAction.TogglePlayPause },
        )
    }

    @Test
    fun settingsOpensWithFirstActionFocused() {
        setContent(
            stateProvider = {
                readyState().copy(settingsPage = PlayerSettingsPage.Root)
            },
        )

        composeRule.onNodeWithTag(PlayerTestTags.SettingsOptionPrefix + "quality")
            .assertIsFocused()
    }

    @Test
    fun settingsPagesDispatchAllSelectionActions() {
        val actions = mutableListOf<PlayerAction>()
        var state by mutableStateOf(
            readyState().copy(settingsPage = PlayerSettingsPage.Quality),
        )
        setContent(
            stateProvider = { state },
            actions = actions,
        )

        composeRule.onNodeWithTag(PlayerTestTags.SettingsOptionPrefix + "automatic")
            .assertIsFocused()
            .press(Key.Enter)
        state = state.copy(settingsPage = PlayerSettingsPage.Audio)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(PlayerTestTags.SettingsOptionPrefix + "audio-en")
            .assertIsFocused()
            .press(Key.Enter)
        state = state.copy(settingsPage = PlayerSettingsPage.Subtitles)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(PlayerTestTags.SettingsOptionPrefix + "automatic")
            .assertIsFocused()
            .press(Key.Enter)
        state = state.copy(settingsPage = PlayerSettingsPage.Speed)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(PlayerTestTags.SettingsOptionPrefix + "0.5")
            .assertIsFocused()
            .press(Key.Enter)
        state = state.copy(settingsPage = PlayerSettingsPage.ResizeMode)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(PlayerTestTags.SettingsOptionPrefix + PlaybackResizeMode.Fit.name)
            .assertIsFocused()
            .press(Key.Enter)

        assertTrue(actions.contains(PlayerAction.SelectVideoTrack(null)))
        assertTrue(actions.contains(PlayerAction.SelectAudioTrack("audio-en")))
        assertTrue(actions.contains(PlayerAction.SelectTextTrack(null)))
        assertTrue(actions.contains(PlayerAction.SelectSpeed(0.5f)))
        assertTrue(actions.contains(PlayerAction.SelectResizeMode(PlaybackResizeMode.Fit)))
    }

    @Test
    fun timelineDpadStartsScrubbingAndShowsFilmstripState() {
        val actions = mutableListOf<PlayerAction>()
        setContent(stateProvider = ::readyState, actions = actions)

        composeRule.onNodeWithTag(PlayerTestTags.Timeline)
            .performKeyInput {
                keyDown(Key.DirectionRight)
                keyUp(Key.DirectionRight)
            }

        assertTrue(actions.any { it == PlayerAction.ScrubStarted })
        assertTrue(actions.any { it is PlayerAction.ScrubChanged })

        setContent(
            stateProvider = {
                readyState().copy(
                    isScrubbing = true,
                    scrubPositionMillis = 70_000L,
                )
            },
        )
        composeRule.onNodeWithTag(PlayerTestTags.Filmstrip).assertIsDisplayed()
    }

    @Test
    fun preparingFocusesBackAndBufferingDoesNotStealEstablishedFocus() {
        var state by mutableStateOf(
            readyState().copy(phase = PlaybackPhase.Preparing, isPlaying = false),
        )
        setContent(stateProvider = { state })

        composeRule.onNodeWithTag(PlayerTestTags.Back).assertIsFocused()

        state = readyState()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(PlayerTestTags.PlayPause)
            .assertIsFocused()
            .press(Key.DirectionLeft)
        composeRule.onNodeWithTag(PlayerTestTags.Rewind).assertIsFocused()

        state = state.copy(phase = PlaybackPhase.Buffering)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(PlayerTestTags.Rewind).assertIsFocused()
    }

    @Test
    fun recoverableErrorFocusesRetryAndDispatchesRetry() {
        val actions = mutableListOf<PlayerAction>()
        setContent(
            stateProvider = {
                readyState().copy(
                    phase = PlaybackPhase.Error,
                    error = PlaybackErrorModel(
                        code = "TEST",
                        message = "Playback failed",
                        isRecoverable = true,
                    ),
                )
            },
            actions = actions,
        )

        composeRule.onNodeWithText("Retry")
            .assertIsFocused()
            .press(Key.Enter)

        assertEquals(listOf(PlayerAction.Retry), actions.filter { it == PlayerAction.Retry })
    }

    @Test
    fun settingsBackReturnsOneLevelThenRestoresSettingsButtonFocus() {
        var state by mutableStateOf(
            readyState().copy(settingsPage = PlayerSettingsPage.Quality),
        )
        setContent(
            stateProvider = { state },
            onAction = { action ->
                if (action == PlayerAction.CloseSettings) {
                    state = state.copy(
                        settingsPage = if (state.settingsPage == PlayerSettingsPage.Root) {
                            null
                        } else {
                            PlayerSettingsPage.Root
                        },
                    )
                }
            },
        )

        pressBack()
        composeRule.onNodeWithTag(PlayerTestTags.SettingsOptionPrefix + "quality")
            .assertIsFocused()

        pressBack()
        composeRule.onNodeWithTag(PlayerTestTags.Settings).assertDoesNotExist()
        composeRule.onNodeWithTag(PlayerTestTags.SettingsButton).assertIsFocused()
    }

    @Test
    fun hiddenControlsWakeOnFirstRemoteKey() {
        val actions = mutableListOf<PlayerAction>()
        setContent(
            stateProvider = { readyState().copy(controlsVisible = false) },
            actions = actions,
        )

        composeRule.onNodeWithTag(PlayerTestTags.Root).press(Key.Enter)

        assertTrue(actions.contains(PlayerAction.ToggleControls))
    }

    private fun setContent(
        stateProvider: () -> PlayerUiState,
        actions: MutableList<PlayerAction> = mutableListOf(),
        onAction: ((PlayerAction) -> Unit)? = null,
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvPlayerScreen(
                    state = stateProvider(),
                    videoSurface = null,
                    onAction = onAction ?: { action ->
                        actions.add(action)
                        Unit
                    },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun readyState(): PlayerUiState {
        return PlayerUiState(
            title = "Orbit Fall",
            phase = PlaybackPhase.Ready,
            isPlaying = true,
            positionMillis = 60_000L,
            durationMillis = 600_000L,
            bufferedPositionMillis = 120_000L,
            videoTracks = listOf(
                PlaybackTrackModel("video-1080", PlaybackTrackType.Video, "1080p"),
            ),
            audioTracks = listOf(
                PlaybackTrackModel("audio-en", PlaybackTrackType.Audio, "English"),
                PlaybackTrackModel("audio-fr", PlaybackTrackType.Audio, "French"),
            ),
            textTracks = listOf(
                PlaybackTrackModel("text-en", PlaybackTrackType.Text, "English"),
            ),
            selectedVideoTrackId = "video-1080",
            selectedAudioTrackId = "audio-en",
        )
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.press(key: Key) {
        performKeyInput {
            keyDown(key)
            keyUp(key)
        }
    }
}
