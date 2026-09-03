@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.pampoukidis.streamcoretv.web.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.events.Event

internal class DiagnosticPlaybackSessionFactory(
    private val scenario: DiagnosticPlayerScenario,
    private val registry: DiagnosticPlayerRegistry = DiagnosticPlayerRegistry,
) : PlaybackSessionFactory {
    override fun create(): PlaybackSession {
        return DiagnosticPlaybackSession(
            scenario = scenario,
            registry = registry,
        )
    }
}

private class DiagnosticPlaybackSession(
    private val scenario: DiagnosticPlayerScenario,
    private val registry: DiagnosticPlayerRegistry,
) : PlaybackSession {
    private val mutableState = MutableStateFlow(PlaybackEngineState())
    private val videoElement = document.createElement("video") as HTMLVideoElement
    private val activityListener: (Event) -> Unit = { }
    private var timerId: Int? = null
    private var closed: Boolean = false
    private var media: PlaybackMediaModel? = null
    private var startPositionMillis: Long = 0L

    override val state = mutableState
    override val videoSurface: PlaybackVideoSurface = DiagnosticPlaybackVideoSurface(videoElement)

    init {
        videoElement.controls = false
        videoElement.preload = "none"
        videoElement.setAttribute("aria-label", "Video playback")
        videoElement.setAttribute("data-testid", "player:video")
        videoElement.setAttribute("style", "pointer-events:none;")
        videoElement.addEventListener(ActivityEvent, activityListener)
        registry.sessionOpened()
    }

    override fun prepare(media: PlaybackMediaModel, startPositionMillis: Long) {
        if (closed) {
            return
        }
        this.media = media
        this.startPositionMillis = startPositionMillis.coerceIn(0L, DurationMillis)
        registry.prepared()
        if (
            scenario == DiagnosticPlayerScenario.RecoverableError &&
            registry.prepareCount == 1
        ) {
            mutableState.value = PlaybackEngineState(
                phase = PlaybackPhase.Error,
                error = PlaybackErrorModel(
                    code = "RAW_ENGINE_FAILURE",
                    message = "Manifest request failed.",
                    isRecoverable = true,
                ),
            )
            return
        }
        mutableState.value = readyState(positionMillis = this.startPositionMillis)
    }

    override fun play() {
        if (closed || mutableState.value.phase != PlaybackPhase.Ready) {
            return
        }
        registry.activated()
        startTimer()
        mutableState.value = mutableState.value.copy(isPlaying = true)
    }

    override fun pause() {
        if (closed) {
            return
        }
        stopTimer()
        mutableState.value = mutableState.value.copy(isPlaying = false)
    }

    override fun seekTo(positionMillis: Long) {
        if (closed) {
            return
        }
        val position = positionMillis.coerceIn(0L, DurationMillis)
        mutableState.value = mutableState.value.copy(
            positionMillis = position,
            bufferedPositionMillis = DurationMillis,
        )
    }

    override fun setSpeed(speed: Float) {
        if (closed) {
            return
        }
        mutableState.value = mutableState.value.copy(speed = speed.coerceIn(0.5f, 2f))
    }

    override fun selectVideoTrack(trackId: String?) {
        if (closed) {
            return
        }
        mutableState.value = mutableState.value.copy(selectedVideoTrackId = trackId)
    }

    override fun selectAudioTrack(trackId: String?) {
        if (closed) {
            return
        }
        mutableState.value = mutableState.value.copy(selectedAudioTrackId = trackId)
    }

    override fun selectTextTrack(trackId: String?) {
        if (closed) {
            return
        }
        mutableState.value = mutableState.value.copy(selectedTextTrackId = trackId)
    }

    override fun setResizeMode(mode: PlaybackResizeMode) {
        if (closed) {
            return
        }
        mutableState.value = mutableState.value.copy(resizeMode = mode)
    }

    override fun retry() {
        val currentMedia = media ?: return
        prepare(currentMedia, startPositionMillis)
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        stopTimer()
        videoElement.removeEventListener(ActivityEvent, activityListener)
        videoElement.pause()
        videoElement.removeAttribute("src")
        videoElement.remove()
        mutableState.value = PlaybackEngineState()
        registry.sessionClosed()
    }

    override fun requestFilmstrip(
        positionsMillis: List<Long>,
    ): Flow<PlaybackFilmstripFrameModel> {
        return emptyFlow()
    }

    private fun readyState(positionMillis: Long): PlaybackEngineState {
        val tracksEnabled = scenario == DiagnosticPlayerScenario.Tracks
        return PlaybackEngineState(
            phase = PlaybackPhase.Ready,
            isPlaying = false,
            positionMillis = positionMillis,
            durationMillis = DurationMillis,
            bufferedPositionMillis = DurationMillis,
            videoAspectRatio = 16f / 9f,
            videoTracks = if (tracksEnabled) VideoTracks else emptyList(),
            audioTracks = if (tracksEnabled) AudioTracks else emptyList(),
            textTracks = if (tracksEnabled) TextTracks else emptyList(),
            selectedVideoTrackId = if (tracksEnabled) Video1080Id else null,
            selectedAudioTrackId = if (tracksEnabled) AudioEnglishId else null,
            selectedTextTrackId = null,
        )
    }

    private fun startTimer() {
        if (timerId != null) {
            return
        }
        timerId = window.setInterval(
            handler = { null },
            timeout = TimerIntervalMillis,
        )
        registry.timerStarted()
    }

    private fun stopTimer() {
        val activeTimerId = timerId ?: return
        window.clearInterval(activeTimerId)
        timerId = null
        registry.timerStopped()
    }

    private companion object {
        const val ActivityEvent = "timeupdate"
        const val DurationMillis = 120_000L
        const val TimerIntervalMillis = 1_000
        const val Video1080Id = "video-1080"
        const val AudioEnglishId = "audio-en"

        val VideoTracks = listOf(
            PlaybackTrackModel(
                id = Video1080Id,
                type = PlaybackTrackType.Video,
                label = "1080p · 5.8 Mbps",
                videoHeight = 1080,
            ),
            PlaybackTrackModel(
                id = "video-720",
                type = PlaybackTrackType.Video,
                label = "720p · 3.2 Mbps",
                videoHeight = 720,
            ),
        )
        val AudioTracks = listOf(
            PlaybackTrackModel(
                id = AudioEnglishId,
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
        val TextTracks = listOf(
            PlaybackTrackModel(
                id = "text-en",
                type = PlaybackTrackType.Text,
                label = "English (CC)",
                language = "en",
            ),
        )
    }
}

private class DiagnosticPlaybackVideoSurface(
    private val videoElement: HTMLVideoElement,
) : PlaybackVideoSurface {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Render(modifier: Modifier) {
        HtmlElementView(
            factory = { videoElement },
            update = { element ->
                element.setAttribute("data-testid", "player:video")
                disableHostPointerEvents(element)
            },
            onRelease = { element -> element.remove() },
            modifier = modifier,
        )
    }

    private fun disableHostPointerEvents(element: HTMLVideoElement) {
        val host = element.parentElement ?: return
        val style = host.getAttribute("style").orEmpty()
        if ("pointer-events" !in style) {
            host.setAttribute("style", "$style;pointer-events:none;")
        }
    }
}
