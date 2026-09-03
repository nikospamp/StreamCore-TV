package com.pampoukidis.streamcoretv.feature.player.web.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerSettingsPage
import com.pampoukidis.streamcoretv.feature.player.common.player.PlayerUiState
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

internal enum class WebPlayerShowcaseScenario {
    Preparing,
    AutoplayActivation,
    Playing,
    Paused,
    Buffering,
    Ended,
    Settings,
    RecoverableError,
    FatalError,
    Scrubbing,
    NoFilmstrip,
    LongText,
    ControlsHidden,
}

internal object WebPlayerFixtures {
    val videoTracks = listOf(
        PlaybackTrackModel(
            id = "video-1080",
            type = PlaybackTrackType.Video,
            label = "1080p · 5.8 Mbps",
            videoHeight = 1_080,
        ),
        PlaybackTrackModel(
            id = "video-720",
            type = PlaybackTrackType.Video,
            label = "720p · 3.1 Mbps",
            videoHeight = 720,
        ),
    )
    val audioTracks = listOf(
        PlaybackTrackModel(
            id = "audio-en",
            type = PlaybackTrackType.Audio,
            label = "English · Original 5.1",
            language = "en",
        ),
        PlaybackTrackModel(
            id = "audio-el",
            type = PlaybackTrackType.Audio,
            label = "Ελληνικά · Stereo",
            language = "el",
        ),
    )
    val textTracks = listOf(
        PlaybackTrackModel(
            id = "text-en",
            type = PlaybackTrackType.Text,
            label = "English (CC)",
            language = "en",
        ),
        PlaybackTrackModel(
            id = "text-el",
            type = PlaybackTrackType.Text,
            label = "Ελληνικά",
            language = "el",
        ),
    )
    val request = PlaybackRequestModel(
        profileId = "web-player-profile",
        contentId = "orbit-fall",
        contentSnapshot = ContentModel(
            id = "orbit-fall",
            title = "Orbit Fall",
            description = "A backend-free player fixture.",
            rating = 9,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
        ),
    )
    val videoSurface: PlaybackVideoSurface = FakeWebPlaybackVideoSurface

    fun state(scenario: WebPlayerShowcaseScenario): PlayerUiState {
        return when (scenario) {
            WebPlayerShowcaseScenario.Preparing -> PlayerUiState(
                title = "Orbit Fall",
                phase = PlaybackPhase.Preparing,
            )

            WebPlayerShowcaseScenario.AutoplayActivation -> readyState(
                isPlaying = false,
            ).copy(positionMillis = 0L)

            WebPlayerShowcaseScenario.Playing -> readyState(isPlaying = true)
            WebPlayerShowcaseScenario.Paused -> readyState(isPlaying = false)
            WebPlayerShowcaseScenario.Buffering -> readyState(isPlaying = true).copy(
                phase = PlaybackPhase.Buffering,
            )

            WebPlayerShowcaseScenario.Ended -> readyState(isPlaying = false).copy(
                phase = PlaybackPhase.Ended,
                positionMillis = FixtureDurationMillis,
            )

            WebPlayerShowcaseScenario.Settings -> readyState(isPlaying = false).copy(
                settingsPage = PlayerSettingsPage.Root,
            )

            WebPlayerShowcaseScenario.RecoverableError -> errorState(recoverable = true)
            WebPlayerShowcaseScenario.FatalError -> errorState(recoverable = false)
            WebPlayerShowcaseScenario.Scrubbing -> readyState(isPlaying = false).copy(
                isScrubbing = true,
                scrubPositionMillis = FixtureScrubPositionMillis,
                filmstripFrames = FixtureFilmstripPositions.map { position ->
                    PlaybackFilmstripFrameModel(positionMillis = position, image = null)
                },
            )

            WebPlayerShowcaseScenario.NoFilmstrip -> readyState(isPlaying = false).copy(
                isScrubbing = true,
                scrubPositionMillis = FixtureScrubPositionMillis,
                filmstripFrames = emptyList(),
            )

            WebPlayerShowcaseScenario.LongText -> readyState(isPlaying = false).copy(
                title = LongFixtureTitle,
                settingsPage = PlayerSettingsPage.Audio,
                audioTracks = listOf(
                    audioTracks.first().copy(label = LongFixtureTrackLabel),
                    audioTracks.last(),
                ),
            )

            WebPlayerShowcaseScenario.ControlsHidden -> readyState(isPlaying = true).copy(
                controlsVisible = false,
            )
        }
    }

    private fun readyState(isPlaying: Boolean): PlayerUiState {
        return PlayerUiState(
            title = "Orbit Fall",
            phase = PlaybackPhase.Ready,
            isPlaying = isPlaying,
            positionMillis = FixturePositionMillis,
            durationMillis = FixtureDurationMillis,
            bufferedPositionMillis = FixtureBufferedPositionMillis,
            videoAspectRatio = FixtureVideoAspectRatio,
            videoTracks = videoTracks,
            audioTracks = audioTracks,
            textTracks = textTracks,
            selectedVideoTrackId = videoTracks.first().id,
            selectedAudioTrackId = audioTracks.first().id,
            selectedTextTrackId = null,
            speed = 1f,
            resizeMode = PlaybackResizeMode.Fit,
        )
    }

    private fun errorState(recoverable: Boolean): PlayerUiState {
        return readyState(isPlaying = false).copy(
            phase = PlaybackPhase.Error,
            error = PlaybackErrorModel(
                code = "PLAYBACK_FAILED",
                message = "Playback failed.",
                isRecoverable = recoverable,
            ),
        )
    }

    private const val FixturePositionMillis = 1_420_000L
    private const val FixtureDurationMillis = 5_400_000L
    private const val FixtureBufferedPositionMillis = 1_680_000L
    private const val FixtureScrubPositionMillis = 2_110_000L
    private const val FixtureVideoAspectRatio = 16f / 9f
    private val FixtureFilmstripPositions = listOf(
        2_100_000L,
        2_105_000L,
        2_110_000L,
        2_115_000L,
        2_120_000L,
    )
    private const val LongFixtureTitle =
        "Orbit Fall: A Chronicle of the Final Rescue Beyond the Last Known Horizon"
    private const val LongFixtureTrackLabel =
        "English descriptive audio with extended accessibility narration and spatial mix"
}

internal class FakeWebPlaybackSession(
    initialState: PlaybackEngineState = PlaybackEngineState(),
) : PlaybackSession {
    private val mutableState = MutableStateFlow(initialState)
    override val state: MutableStateFlow<PlaybackEngineState> = mutableState
    override val videoSurface: PlaybackVideoSurface = FakeWebPlaybackVideoSurface

    var prepareCalls: Int = 0
        private set
    var playCalls: Int = 0
        private set
    var pauseCalls: Int = 0
        private set
    var retryCalls: Int = 0
        private set
    var closeCalls: Int = 0
        private set
    var lastSeekPositionMillis: Long? = null
        private set
    var lastSpeed: Float? = null
        private set
    var lastVideoTrackId: String? = null
        private set
    var lastAudioTrackId: String? = null
        private set
    var lastTextTrackId: String? = null
        private set
    var lastResizeMode: PlaybackResizeMode? = null
        private set

    override fun prepare(media: PlaybackMediaModel, startPositionMillis: Long) {
        prepareCalls += 1
    }

    override fun play() {
        playCalls += 1
    }

    override fun pause() {
        pauseCalls += 1
    }

    override fun seekTo(positionMillis: Long) {
        lastSeekPositionMillis = positionMillis
    }

    override fun setSpeed(speed: Float) {
        lastSpeed = speed
    }

    override fun selectVideoTrack(trackId: String?) {
        lastVideoTrackId = trackId
    }

    override fun selectAudioTrack(trackId: String?) {
        lastAudioTrackId = trackId
    }

    override fun selectTextTrack(trackId: String?) {
        lastTextTrackId = trackId
    }

    override fun setResizeMode(mode: PlaybackResizeMode) {
        lastResizeMode = mode
    }

    override fun retry() {
        retryCalls += 1
    }

    override fun requestFilmstrip(positionsMillis: List<Long>): Flow<PlaybackFilmstripFrameModel> {
        return emptyFlow()
    }

    override fun close() {
        closeCalls += 1
    }

    fun emit(state: PlaybackEngineState) {
        mutableState.value = state
    }
}

private object FakeWebPlaybackVideoSurface : PlaybackVideoSurface {
    @Composable
    override fun Render(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        )
    }
}
