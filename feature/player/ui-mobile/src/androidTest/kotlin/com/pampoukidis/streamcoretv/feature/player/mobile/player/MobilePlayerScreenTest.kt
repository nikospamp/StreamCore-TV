package com.pampoukidis.streamcoretv.feature.player.mobile.player

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerAction
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.feature.player.common.testing.PlayerTestTags
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MobilePlayerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playbackStatesExposeExpectedControls() {
        var state by mutableStateOf(PlayerUiState(phase = PlaybackPhase.Preparing))
        setPlayerContent(state = { state })
        composeRule.onNodeWithTag(PlayerTestTags.Buffering).assertExists()

        state = readyState(isPlaying = true)
        composeRule.onAllNodesWithContentDescription("Pause")[0].assertExists()

        state = readyState(isPlaying = false)
        composeRule.onAllNodesWithContentDescription("Play")[0].assertExists()

        state = readyState().copy(phase = PlaybackPhase.Buffering)
        composeRule.onNodeWithTag(PlayerTestTags.Buffering).assertExists()

        state = readyState().copy(
            isScrubbing = true,
            scrubPositionMillis = 42_000L,
        )
        composeRule.onNodeWithTag(PlayerTestTags.Filmstrip).assertExists()
        composeRule.onNodeWithText("0:42").assertExists()

        state = readyState().copy(phase = PlaybackPhase.Ended)
        composeRule.onNodeWithContentDescription("Replay").assertExists()

        state = readyState().copy(
            phase = PlaybackPhase.Error,
            error = PlaybackErrorModel(
                code = "test",
                message = "Playback failed",
                isRecoverable = true,
            ),
        )
        composeRule.onNodeWithTag(PlayerTestTags.Error).assertExists()
        composeRule.onNodeWithText("Retry").assertExists()
    }

    @Test
    fun settingsExposeMeaningfulCategoriesAndForwardSelection() {
        val actions = mutableListOf<PlayerAction>()
        val videoTrack = PlaybackTrackModel(
            id = "video-1080",
            type = PlaybackTrackType.Video,
            label = "1080p",
        )
        var state by mutableStateOf(
            readyState().copy(
                settingsPage = PlayerSettingsPage.Root,
                videoTracks = listOf(videoTrack),
                selectedVideoTrackId = videoTrack.id,
                audioTracks = listOf(
                    PlaybackTrackModel("audio-en", PlaybackTrackType.Audio, "English"),
                    PlaybackTrackModel("audio-fr", PlaybackTrackType.Audio, "French"),
                ),
                textTracks = listOf(
                    PlaybackTrackModel("text-en", PlaybackTrackType.Text, "English"),
                ),
            )
        )
        setPlayerContent(state = { state }, onAction = actions::add)

        composeRule.onNodeWithTag(PlayerTestTags.Settings).assertExists()
        composeRule.onNodeWithText("Playback preferences").assertExists()
        composeRule.onNodeWithText("1080p").assertExists()
        composeRule.onNodeWithContentDescription("Dismiss playback settings")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(actions.contains(PlayerAction.BackSelected))

        composeRule.onNodeWithText("Quality")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(actions.contains(PlayerAction.OpenSettings(PlayerSettingsPage.Quality)))

        state = state.copy(settingsPage = PlayerSettingsPage.Quality)
        composeRule.onNodeWithText("Video resolution and data usage").assertExists()
        composeRule.onNodeWithText("1080p")
            .assertIsSelected()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(actions.contains(PlayerAction.SelectVideoTrack(videoTrack.id)))
    }

    @Test
    fun automaticQualityIsSelectedWithoutAnExplicitVideoOverride() {
        val videoTrack = PlaybackTrackModel(
            id = "video-1080",
            type = PlaybackTrackType.Video,
            label = "1080p",
        )
        setPlayerContent(
            state = {
                readyState().copy(
                    settingsPage = PlayerSettingsPage.Quality,
                    videoTracks = listOf(videoTrack),
                    selectedVideoTrackId = null,
                )
            },
        )

        composeRule.onNodeWithText("Auto").assertIsSelected()
    }

    @Test
    fun playerActionsMeetMinimumTouchTarget() {
        setPlayerContent(state = { readyState().copy(isPipSupported = true) })

        composeRule.onNodeWithContentDescription("Back").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Playback settings").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("Picture in picture").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun phoneLandscapeRealTouchTargetsDispatchPlayerActions() {
        val actions = mutableListOf<PlayerAction>()
        var state by mutableStateOf(
            readyState(isPlaying = true).copy(controlsVisible = false),
        )
        setPhoneLandscapePlayerContent(
            state = { state },
            onAction = { action ->
                actions += action
                state = reduceTouchAction(state = state, action = action)
            },
        )

        composeRule.onNodeWithTag(PlayerTestTags.Root).performTouchInput {
            click(center)
        }
        composeRule.mainClock.advanceTimeBy(SurfaceTapSettleMillis)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithContentDescription("Pause")[0].performTouchInput {
            click(center)
        }
        composeRule.onAllNodesWithContentDescription("Play")[0].performTouchInput {
            click(center)
        }
        composeRule.onNodeWithContentDescription("Back 10 seconds").performTouchInput {
            click(center)
        }
        composeRule.onNodeWithContentDescription("Forward 10 seconds").performTouchInput {
            click(center)
        }
        composeRule.onNodeWithTag(PlayerTestTags.Timeline).performTouchInput {
            click(Offset(x = width * 0.75f, y = height / 2f))
        }
        composeRule.onNodeWithContentDescription("Playback settings").performTouchInput {
            click(center)
        }

        assertEquals(1, actions.count { it == PlayerAction.ToggleControls })
        assertEquals(2, actions.count { it == PlayerAction.TogglePlayPause })
        assertTrue(actions.contains(PlayerAction.SeekBy(deltaMillis = -10_000L)))
        assertTrue(actions.contains(PlayerAction.SeekBy(deltaMillis = 10_000L)))
        assertTrue(actions.contains(PlayerAction.ScrubStarted))
        assertTrue(actions.any { it is PlayerAction.ScrubChanged })
        assertTrue(actions.contains(PlayerAction.ScrubFinished))
        assertTrue(actions.contains(PlayerAction.OpenSettings()))
        assertTrue(state.positionMillis > PhoneLandscapeInitialPositionMillis)
        assertEquals(PlayerSettingsPage.Root, state.settingsPage)
    }

    @Test
    fun tabletTouchControlsForwardPauseSeekSettingsAndExitActions() {
        val actions = mutableListOf<PlayerAction>()
        var state by mutableStateOf(readyState(isPlaying = true).copy(isPipSupported = true))
        setTabletPlayerContent(
            state = { state },
            onAction = { action ->
                actions += action
                if (action == PlayerAction.TogglePlayPause) {
                    state = state.copy(isPlaying = !state.isPlaying)
                }
            },
        )

        composeRule.onAllNodesWithContentDescription("Pause")[0]
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onAllNodesWithContentDescription("Play")[0]
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithContentDescription("Back 10 seconds")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithContentDescription("Playback settings")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithContentDescription("Back")
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(
            listOf(
                PlayerAction.TogglePlayPause,
                PlayerAction.TogglePlayPause,
                PlayerAction.SeekBy(deltaMillis = -10_000L),
                PlayerAction.OpenSettings(),
                PlayerAction.BackSelected,
            ),
            actions,
        )
    }

    @Test
    fun tabletSettingsPanelUsesBoundedTouchLayoutAndExposesPages() {
        setTabletPlayerContent(
            state = {
                readyState().copy(
                    settingsPage = PlayerSettingsPage.Root,
                    videoTracks = listOf(
                        PlaybackTrackModel("video-1080", PlaybackTrackType.Video, "1080p"),
                    ),
                )
            },
        )

        composeRule.onNodeWithTag(PlayerTestTags.Settings)
            .assertWidthIsEqualTo(TabletSettingsPanelWidthDp.dp)
        composeRule.onNodeWithText("Quality").assertExists()
        composeRule.onNodeWithText("Speed").assertExists()
        composeRule.onNodeWithText("Resize mode").assertExists()
    }

    private fun setPlayerContent(
        state: () -> PlayerUiState,
        onAction: (PlayerAction) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobilePlayerScreen(
                    state = state(),
                    videoSurface = FakeVideoSurface,
                    onAction = onAction,
                )
            }
        }
    }

    private fun setTabletPlayerContent(
        state: () -> PlayerUiState,
        onAction: (PlayerAction) -> Unit = {},
    ) {
        composeRule.setContent {
            val currentConfiguration = LocalConfiguration.current
            val tabletConfiguration = remember(currentConfiguration) {
                Configuration(currentConfiguration).apply {
                    orientation = Configuration.ORIENTATION_LANDSCAPE
                    screenWidthDp = TabletWidthDp
                    screenHeightDp = TabletHeightDp
                }
            }
            CompositionLocalProvider(LocalConfiguration provides tabletConfiguration) {
                StreamCoreTheme(darkTheme = true) {
                    MobilePlayerScreen(
                        state = state(),
                        videoSurface = FakeVideoSurface,
                        onAction = onAction,
                    )
                }
            }
        }
    }

    private fun setPhoneLandscapePlayerContent(
        state: () -> PlayerUiState,
        onAction: (PlayerAction) -> Unit,
    ) {
        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(
                    DpSize(
                        width = PhoneLandscapeWidthDp.dp,
                        height = PhoneLandscapeHeightDp.dp,
                    ),
                ),
            ) {
                StreamCoreTheme(darkTheme = true) {
                    MobilePlayerScreen(
                        state = state(),
                        videoSurface = FakeVideoSurface,
                        onAction = onAction,
                    )
                }
            }
        }
    }

    private fun reduceTouchAction(
        state: PlayerUiState,
        action: PlayerAction,
    ): PlayerUiState {
        return when (action) {
            PlayerAction.ToggleControls -> state.copy(
                controlsVisible = !state.controlsVisible,
            )

            PlayerAction.TogglePlayPause -> state.copy(
                isPlaying = !state.isPlaying,
            )

            is PlayerAction.SeekBy -> state.copy(
                positionMillis = (state.positionMillis + action.deltaMillis)
                    .coerceIn(0L, state.durationMillis),
            )

            PlayerAction.ScrubStarted -> state.copy(
                isScrubbing = true,
                scrubPositionMillis = state.positionMillis,
            )

            is PlayerAction.ScrubChanged -> state.copy(
                scrubPositionMillis = action.positionMillis,
            )

            PlayerAction.ScrubFinished -> state.copy(
                isScrubbing = false,
                positionMillis = state.scrubPositionMillis,
            )

            is PlayerAction.OpenSettings -> state.copy(settingsPage = action.page)
            else -> state
        }
    }

    private fun readyState(isPlaying: Boolean = false): PlayerUiState {
        return PlayerUiState(
            title = "Sintel",
            phase = PlaybackPhase.Ready,
            isPlaying = isPlaying,
            controlsVisible = true,
            positionMillis = 30_000L,
            durationMillis = 120_000L,
            bufferedPositionMillis = 60_000L,
        )
    }

    private object FakeVideoSurface : PlaybackVideoSurface {
        @Composable
        override fun Render(modifier: Modifier) {
            Box(modifier)
        }
    }

    private companion object {
        const val PhoneLandscapeWidthDp = 914
        const val PhoneLandscapeHeightDp = 411
        const val PhoneLandscapeInitialPositionMillis = 30_000L
        const val SurfaceTapSettleMillis = 500L
        const val TabletWidthDp = 1_024
        const val TabletHeightDp = 600
        const val TabletSettingsPanelWidthDp = 410
    }
}
