package com.pampoukidis.streamcoretv.playback.web

import androidx.compose.ui.graphics.decodeToImageBitmap
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.io.encoding.Base64

internal class WebPlaybackSession(
    private val bridge: WebPlaybackBridge,
    override val videoSurface: PlaybackVideoSurface,
) : PlaybackSession, WebPlaybackBridgeListener {
    private val mutableState = MutableStateFlow(
        PlaybackEngineState(
            phase = PlaybackPhase.Idle,
            isPlaying = false,
        ),
    )

    override val state: StateFlow<PlaybackEngineState> = mutableState.asStateFlow()

    internal var isClosed: Boolean = false
        private set
    private var generation: Int = 0
    private var acceptsBridgeSnapshots: Boolean = true
    private var currentMedia: PlaybackMediaModel? = null
    private var requestedStartPositionMillis: Long = 0L

    init {
        bridge.setListener(this)
    }

    override fun prepare(media: PlaybackMediaModel, startPositionMillis: Long) {
        if (isClosed) {
            return
        }
        generation += 1
        acceptsBridgeSnapshots = false
        currentMedia = media
        requestedStartPositionMillis = startPositionMillis.coerceAtLeast(0L)
        val currentState = mutableState.value
        mutableState.value = PlaybackEngineState(
            phase = PlaybackPhase.Preparing,
            speed = currentState.speed,
            resizeMode = currentState.resizeMode,
        )
        val uri = media.uri?.takeIf(String::isNotBlank)
        if (uri == null) {
            bridge.reset(generation)
            mutableState.value = PlaybackEngineState(
                phase = PlaybackPhase.Error,
                speed = currentState.speed,
                resizeMode = currentState.resizeMode,
                error = PlaybackErrorModel(
                    code = SourceMissingCode,
                    message = SourceMissingMessage,
                    isRecoverable = true,
                ),
            )
            return
        }
        acceptsBridgeSnapshots = true
        bridge.load(
            generation = generation,
            uri = uri,
            mimeType = media.mimeType,
            startPositionMillis = requestedStartPositionMillis,
        )
    }

    override fun play() {
        if (isClosed) {
            return
        }
        if (mutableState.value.phase == PlaybackPhase.Ended) {
            bridge.seekTo(0L)
        }
        bridge.play()
    }

    override fun pause() {
        if (isClosed) {
            return
        }
        bridge.pause()
    }

    override fun seekTo(positionMillis: Long) {
        if (isClosed) {
            return
        }
        val durationMillis = mutableState.value.durationMillis
        val maximum = durationMillis.takeIf { duration -> duration > 0L } ?: Long.MAX_VALUE
        bridge.seekTo(positionMillis.coerceIn(0L, maximum))
    }

    override fun setSpeed(speed: Float) {
        if (isClosed) {
            return
        }
        val safeSpeed = speed.coerceIn(MinSpeed, MaxSpeed)
        mutableState.value = mutableState.value.copy(speed = safeSpeed)
        bridge.setSpeed(safeSpeed)
    }

    override fun selectVideoTrack(trackId: String?) {
        if (isClosed) {
            return
        }
        bridge.selectVideoTrack(trackId)
    }

    override fun selectAudioTrack(trackId: String?) {
        if (isClosed) {
            return
        }
        bridge.selectAudioTrack(trackId)
    }

    override fun selectTextTrack(trackId: String?) {
        if (isClosed) {
            return
        }
        bridge.selectTextTrack(trackId)
    }

    override fun setResizeMode(mode: PlaybackResizeMode) {
        if (isClosed) {
            return
        }
        mutableState.value = mutableState.value.copy(resizeMode = mode)
        bridge.setResizeMode(mode)
    }

    override fun retry() {
        if (isClosed) {
            return
        }
        val media = currentMedia ?: return
        val retryPositionMillis = mutableState.value.positionMillis
            .takeIf { position -> position > 0L }
            ?: requestedStartPositionMillis
        prepare(media, retryPositionMillis)
    }

    override fun requestFilmstrip(
        positionsMillis: List<Long>,
    ): Flow<PlaybackFilmstripFrameModel> {
        val positions = positionsMillis
            .asSequence()
            .map { position -> position.coerceAtLeast(0L) }
            .distinct()
            .toList()
        if (positions.isEmpty()) {
            return emptyFlow()
        }
        return callbackFlow {
            if (isClosed) {
                this.close()
                return@callbackFlow
            }
            val requestGeneration = generation
            val producer = this
            val requestId = bridge.requestFilmstrip(
                generation = requestGeneration,
                positionsMillis = positions,
                listener = object : WebFilmstripListener {
                    override fun onFrame(frame: WebFilmstripFrameSnapshot) {
                        if (isClosed || generation != requestGeneration) {
                            return
                        }
                        val image = runCatching {
                            Base64.Default.decode(frame.encodedPng).decodeToImageBitmap()
                        }.getOrNull() ?: return
                        producer.trySend(
                            PlaybackFilmstripFrameModel(
                                positionMillis = frame.positionMillis.coerceAtLeast(0L),
                                image = image,
                            ),
                        )
                    }

                    override fun onComplete() {
                        producer.close()
                    }
                },
            )
            if (requestId == null) {
                this.close()
                return@callbackFlow
            }
            awaitClose { bridge.cancelFilmstrip(requestId) }
        }
    }

    override fun close() {
        if (isClosed) {
            return
        }
        isClosed = true
        generation += 1
        acceptsBridgeSnapshots = false
        currentMedia = null
        bridge.setListener(null)
        bridge.close()
        mutableState.value = PlaybackEngineState()
    }

    override fun onSnapshot(generation: Int, snapshot: WebPlaybackSnapshot) {
        if (isClosed || !acceptsBridgeSnapshots || generation != this.generation) {
            return
        }
        val failure = snapshot.failure
        mutableState.value = PlaybackEngineState(
            phase = if (failure != null) PlaybackPhase.Error else snapshot.phase.toPlaybackPhase(),
            isPlaying = failure == null && snapshot.isPlaying,
            positionMillis = snapshot.positionMillis.coerceAtLeast(0L),
            durationMillis = snapshot.durationMillis.coerceAtLeast(0L),
            bufferedPositionMillis = snapshot.bufferedPositionMillis.coerceAtLeast(0L),
            videoAspectRatio = snapshot.videoAspectRatio?.takeIf { ratio -> ratio > 0f },
            speed = snapshot.speed.coerceIn(MinSpeed, MaxSpeed),
            resizeMode = snapshot.resizeMode,
            videoTracks = snapshot.videoTracks.map(WebPlaybackTrackSnapshot::toModel),
            audioTracks = snapshot.audioTracks.map(WebPlaybackTrackSnapshot::toModel),
            textTracks = snapshot.textTracks.map(WebPlaybackTrackSnapshot::toModel),
            selectedVideoTrackId = snapshot.selectedVideoTrackId,
            selectedAudioTrackId = snapshot.selectedAudioTrackId,
            selectedTextTrackId = snapshot.selectedTextTrackId,
            error = failure?.let { mappedFailure ->
                PlaybackErrorModel(
                    code = PlaybackFailedCode,
                    message = PlaybackFailedMessage,
                    isRecoverable = mappedFailure.isRecoverable,
                )
            },
        )
    }

    private companion object {
        const val MinSpeed = 0.5f
        const val MaxSpeed = 2f
        const val SourceMissingCode = "SOURCE_MISSING"
        const val SourceMissingMessage = "No playable source is available."
        const val PlaybackFailedCode = "PLAYBACK_FAILED"
        const val PlaybackFailedMessage = "Playback failed."
    }
}

private fun WebPlaybackPhase.toPlaybackPhase(): PlaybackPhase {
    return when (this) {
        WebPlaybackPhase.Idle -> PlaybackPhase.Idle
        WebPlaybackPhase.Preparing -> PlaybackPhase.Preparing
        WebPlaybackPhase.Buffering -> PlaybackPhase.Buffering
        WebPlaybackPhase.Ready -> PlaybackPhase.Ready
        WebPlaybackPhase.Ended -> PlaybackPhase.Ended
        WebPlaybackPhase.Error -> PlaybackPhase.Error
    }
}

private fun WebPlaybackTrackSnapshot.toModel(): PlaybackTrackModel {
    return PlaybackTrackModel(
        id = id,
        type = type,
        label = label,
        language = language,
        videoHeight = videoHeight,
        isSupported = isSupported,
    )
}
