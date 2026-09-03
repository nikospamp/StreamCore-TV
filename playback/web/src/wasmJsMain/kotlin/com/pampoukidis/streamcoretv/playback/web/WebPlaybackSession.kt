package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

internal class WebPlaybackSession : PlaybackSession {
    private val mutableState = MutableStateFlow(
        PlaybackEngineState(
            phase = PlaybackPhase.Idle,
            isPlaying = false,
        ),
    )

    override val state: StateFlow<PlaybackEngineState> = mutableState.asStateFlow()
    override val videoSurface: PlaybackVideoSurface = WebPlaybackVideoSurface()

    internal var isClosed: Boolean = false
        private set

    override fun prepare(media: PlaybackMediaModel, startPositionMillis: Long) {
        // Contract-freeze shell: no browser engine is owned yet.
    }

    override fun play() {
        // Contract-freeze shell: state intentionally remains Idle and non-playing.
    }

    override fun pause() {
        // Contract-freeze shell: state intentionally remains Idle and non-playing.
    }

    override fun seekTo(positionMillis: Long) {
        // Contract-freeze shell: no playback position is owned yet.
    }

    override fun setSpeed(speed: Float) {
        // Contract-freeze shell: no playback speed is owned yet.
    }

    override fun selectVideoTrack(trackId: String?) {
        // Contract-freeze shell: no tracks are owned yet.
    }

    override fun selectAudioTrack(trackId: String?) {
        // Contract-freeze shell: no tracks are owned yet.
    }

    override fun selectTextTrack(trackId: String?) {
        // Contract-freeze shell: no tracks are owned yet.
    }

    override fun setResizeMode(mode: PlaybackResizeMode) {
        // Contract-freeze shell: no video element is owned yet.
    }

    override fun retry() {
        // Contract-freeze shell: no failed browser operation exists to retry.
    }

    override fun requestFilmstrip(
        positionsMillis: List<Long>,
    ): Flow<PlaybackFilmstripFrameModel> {
        return emptyFlow()
    }

    override fun close() {
        if (isClosed) {
            return
        }
        isClosed = true
    }
}
