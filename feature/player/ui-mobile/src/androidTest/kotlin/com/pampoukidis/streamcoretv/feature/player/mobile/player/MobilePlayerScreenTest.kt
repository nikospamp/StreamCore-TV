package com.pampoukidis.streamcoretv.feature.player.mobile.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithContentDescription("Dismiss playback settings").performClick()
        assertTrue(actions.contains(PlayerAction.BackSelected))

        composeRule.onNodeWithText("Quality").performClick()
        assertTrue(actions.contains(PlayerAction.OpenSettings(PlayerSettingsPage.Quality)))

        state = state.copy(settingsPage = PlayerSettingsPage.Quality)
        composeRule.onNodeWithText("Video resolution and data usage").assertExists()
        composeRule.onNodeWithText("1080p").assertIsSelected().performClick()
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
}
