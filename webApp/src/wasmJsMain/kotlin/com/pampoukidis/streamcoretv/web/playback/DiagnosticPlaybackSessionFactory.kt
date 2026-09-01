package com.pampoukidis.streamcoretv.web.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

class DiagnosticPlaybackSessionFactory : PlaybackSessionFactory {
    override fun create(): PlaybackSession {
        return DiagnosticPlaybackSession()
    }
}

private class DiagnosticPlaybackSession : PlaybackSession {
    override val state = MutableStateFlow(PlaybackEngineState())
    override val videoSurface: PlaybackVideoSurface = DiagnosticVideoSurface

    override fun prepare(media: PlaybackMediaModel, startPositionMillis: Long) {
        return
    }

    override fun play() {
        return
    }

    override fun pause() {
        return
    }

    override fun seekTo(positionMillis: Long) {
        return
    }

    override fun setSpeed(speed: Float) {
        return
    }

    override fun selectVideoTrack(trackId: String?) {
        return
    }

    override fun selectAudioTrack(trackId: String?) {
        return
    }

    override fun selectTextTrack(trackId: String?) {
        return
    }

    override fun setResizeMode(mode: PlaybackResizeMode) {
        return
    }

    override fun retry() {
        return
    }

    override fun close() {
        return
    }

    override fun requestFilmstrip(
        positionsMillis: List<Long>,
    ): Flow<PlaybackFilmstripFrameModel> {
        return emptyFlow()
    }
}

private object DiagnosticVideoSurface : PlaybackVideoSurface {
    @Composable
    override fun Render(modifier: Modifier) {
        return
    }
}
