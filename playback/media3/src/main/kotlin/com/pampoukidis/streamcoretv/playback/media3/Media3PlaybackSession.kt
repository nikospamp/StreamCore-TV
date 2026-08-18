package com.pampoukidis.streamcoretv.playback.media3

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.inspector.frame.FrameExtractor
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
internal class Media3PlaybackSession(
    context: Context,
) : PlaybackSession, Player.Listener {

    private val applicationContext = context.applicationContext
    private val player = ExoPlayer.Builder(applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        setHandleAudioBecomingNoisy(true)
        setSeekParameters(SeekParameters.CLOSEST_SYNC)
        addListener(this@Media3PlaybackSession)
    }
    private val mediaSession = MediaSession.Builder(applicationContext, player).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackEngineState())
    override val state: StateFlow<PlaybackEngineState> = _state.asStateFlow()
    override val videoSurface: PlaybackVideoSurface = Media3PlaybackVideoSurface(player)

    private val frameMutex = Mutex()
    private val frameCache = object : LruCache<Long, Bitmap>(FrameCacheBytes) {
        override fun sizeOf(key: Long, value: Bitmap): Int {
            return value.allocationByteCount
        }
    }
    private var frameExtractor: FrameExtractor? = null
    private var currentMediaItem: MediaItem? = null
    private var tickerJob: Job? = null
    private var resizeMode = PlaybackResizeMode.Fit
    private var selectedVideoTrackOverrideId: String? = null
    private var closed = false

    init {
        tickerJob = scope.launch {
            while (isActive) {
                publishState()
                delay(PositionUpdateMillis)
            }
        }
    }

    override fun prepare(media: PlaybackMediaModel, startPositionMillis: Long) {
        val uri = media.uri ?: run {
            publishSourceError()
            return
        }
        val item = MediaItem.Builder()
            .setMediaId(media.assetId)
            .setUri(Uri.parse(uri))
            .setMimeType(media.mimeType)
            .build()
        selectedVideoTrackOverrideId = null
        currentMediaItem = item
        replaceFrameExtractor(item)
        _state.value = _state.value.copy(phase = PlaybackPhase.Preparing, error = null)
        player.setMediaItem(item, startPositionMillis.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = true
    }

    override fun play() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
        }
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekTo(positionMillis: Long) {
        val duration = player.duration.takeUnless { it == C.TIME_UNSET } ?: Long.MAX_VALUE
        player.seekTo(positionMillis.coerceIn(0L, duration))
    }

    override fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(MinSpeed, MaxSpeed))
    }

    override fun selectVideoTrack(trackId: String?) {
        selectedVideoTrackOverrideId = selectTrack(C.TRACK_TYPE_VIDEO, trackId)
        publishState()
    }

    override fun selectAudioTrack(trackId: String?) {
        selectTrack(C.TRACK_TYPE_AUDIO, trackId)
    }

    override fun selectTextTrack(trackId: String?) {
        val parameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, trackId == null)
        findTrack(trackId, C.TRACK_TYPE_TEXT)?.let { selection ->
            parameters
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .addOverride(TrackSelectionOverride(selection.group.mediaTrackGroup, selection.index))
        }
        player.trackSelectionParameters = parameters.build()
    }

    override fun setResizeMode(mode: PlaybackResizeMode) {
        resizeMode = mode
        publishState()
    }

    override fun retry() {
        player.prepare()
        player.playWhenReady = true
    }

    override suspend fun requestFilmstrip(
        positionsMillis: List<Long>,
    ): List<PlaybackFilmstripFrameModel> {
        return frameMutex.withLock {
            val extractor = frameExtractor
            positionsMillis.map { requestedPosition ->
                val key = requestedPosition.coerceAtLeast(0L)
                val cached = frameCache.get(key)
                val bitmap = cached ?: extractor?.let { activeExtractor ->
                    runCatching {
                        val frame = activeExtractor.getFrame(key).await()
                        scaleFrame(frame.bitmap).also { scaled -> frameCache.put(key, scaled) }
                    }.getOrNull()
                }
                PlaybackFilmstripFrameModel(
                    positionMillis = key,
                    image = bitmap?.asImageBitmap(),
                )
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        publishState()
    }

    override fun onPlayerError(error: PlaybackException) {
        publishState()
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        tickerJob?.cancel()
        scope.cancel()
        frameExtractor?.close()
        frameExtractor = null
        frameCache.evictAll()
        mediaSession.release()
        player.removeListener(this)
        player.release()
    }

    private fun selectTrack(trackType: Int, trackId: String?): String? {
        val parameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(trackType)
            .setTrackTypeDisabled(trackType, false)
        val selection = findTrack(trackId, trackType)
        selection?.let { selectedTrack ->
            parameters.addOverride(
                TrackSelectionOverride(selectedTrack.group.mediaTrackGroup, selectedTrack.index),
            )
        }
        player.trackSelectionParameters = parameters.build()
        return selection?.let { selectedTrack ->
            trackId(selectedTrack.group, selectedTrack.index)
        }
    }

    private fun findTrack(trackId: String?, trackType: Int): TrackSelection? {
        if (trackId == null) {
            return null
        }
        player.currentTracks.groups
            .filter { group -> group.type == trackType }
            .forEach { group ->
                for (index in 0 until group.length) {
                    if (trackId(group, index) == trackId && group.isTrackSupported(index)) {
                        return TrackSelection(group, index)
                    }
                }
            }
        return null
    }

    private fun publishState() {
        if (closed) {
            return
        }
        val error = player.playerError
        val tracks = player.currentTracks
        val videoSize = player.videoSize
        _state.value = PlaybackEngineState(
            phase = when {
                error != null -> PlaybackPhase.Error
                player.playbackState == Player.STATE_IDLE -> PlaybackPhase.Idle
                player.playbackState == Player.STATE_BUFFERING -> PlaybackPhase.Buffering
                player.playbackState == Player.STATE_ENDED -> PlaybackPhase.Ended
                player.playbackState == Player.STATE_READY -> PlaybackPhase.Ready
                else -> PlaybackPhase.Preparing
            },
            isPlaying = player.isPlaying,
            positionMillis = player.currentPosition.coerceAtLeast(0L),
            durationMillis = player.duration.validTime(),
            bufferedPositionMillis = player.bufferedPosition.coerceAtLeast(0L),
            videoAspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
                videoSize.width.toFloat() * videoSize.pixelWidthHeightRatio / videoSize.height.toFloat()
            } else {
                null
            },
            speed = player.playbackParameters.speed,
            resizeMode = resizeMode,
            videoTracks = tracks.toModels(C.TRACK_TYPE_VIDEO, PlaybackTrackType.Video),
            audioTracks = tracks.toModels(C.TRACK_TYPE_AUDIO, PlaybackTrackType.Audio),
            textTracks = tracks.toModels(C.TRACK_TYPE_TEXT, PlaybackTrackType.Text),
            selectedVideoTrackId = selectedVideoTrackOverrideId,
            selectedAudioTrackId = tracks.selectedId(C.TRACK_TYPE_AUDIO),
            selectedTextTrackId = tracks.selectedId(C.TRACK_TYPE_TEXT),
            error = error?.let { playbackError ->
                PlaybackErrorModel(
                    code = playbackError.errorCodeName,
                    message = playbackError.localizedMessage ?: "Playback failed",
                    isRecoverable = true,
                )
            },
        )
    }

    private fun Tracks.toModels(
        mediaTrackType: Int,
        modelTrackType: PlaybackTrackType,
    ): List<PlaybackTrackModel> {
        return groups
            .asSequence()
            .filter { group -> group.type == mediaTrackType }
            .flatMap { group ->
                (0 until group.length).asSequence().mapNotNull { index ->
                    if (!group.isTrackSupported(index)) {
                        return@mapNotNull null
                    }
                    val format = group.getTrackFormat(index)
                    PlaybackTrackModel(
                        id = trackId(group, index),
                        type = modelTrackType,
                        label = format.label
                            ?: format.language
                            ?: format.height.takeIf { it > 0 }?.let { height -> "${height}p" }
                            ?: "Track ${index + 1}",
                        language = format.language,
                        videoHeight = format.height.takeIf { it > 0 },
                    )
                }
            }
            .distinctBy { track -> track.videoHeight?.let { "height:$it" } ?: track.id }
            .toList()
    }

    private fun Tracks.selectedId(trackType: Int): String? {
        groups.filter { group -> group.type == trackType }.forEach { group ->
            for (index in 0 until group.length) {
                if (group.isTrackSelected(index)) {
                    return trackId(group, index)
                }
            }
        }
        return null
    }

    private fun trackId(group: Tracks.Group, index: Int): String {
        return "${group.mediaTrackGroup.id}:$index"
    }

    private fun replaceFrameExtractor(mediaItem: MediaItem) {
        frameExtractor?.close()
        frameCache.evictAll()
        frameExtractor = FrameExtractor.Builder(applicationContext, mediaItem)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
    }

    private fun scaleFrame(bitmap: Bitmap): Bitmap {
        if (bitmap.height <= FilmstripHeightPx) {
            return bitmap
        }
        val width = (bitmap.width.toFloat() * FilmstripHeightPx / bitmap.height)
            .toInt()
            .coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, FilmstripHeightPx, true)
    }

    private fun publishSourceError() {
        _state.value = PlaybackEngineState(
            phase = PlaybackPhase.Error,
            error = PlaybackErrorModel(
                code = "SOURCE_MISSING",
                message = "No playable source is available.",
                isRecoverable = true,
            ),
        )
    }

    private fun Long.validTime(): Long {
        return takeUnless { value -> value == C.TIME_UNSET || value < 0L } ?: 0L
    }

    private data class TrackSelection(
        val group: Tracks.Group,
        val index: Int,
    )

    private companion object {
        const val PositionUpdateMillis = 500L
        const val MinSpeed = 0.5f
        const val MaxSpeed = 2f
        const val FilmstripHeightPx = 180
        const val FrameCacheBytes = 8 * 1024 * 1024
    }
}

private suspend fun <T> ListenableFuture<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addListener({ completeContinuation(continuation) }, { runnable -> runnable.run() })
        continuation.invokeOnCancellation { cancel(true) }
    }
}

private fun <T> ListenableFuture<T>.completeContinuation(
    continuation: CancellableContinuation<T>,
) {
    runCatching { get() }
        .onSuccess { value -> continuation.resume(value) }
        .onFailure { throwable -> continuation.resumeWithException(throwable) }
}
