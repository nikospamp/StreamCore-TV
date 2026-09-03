package com.pampoukidis.streamcoretv.playback.media3

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.image.ImageOutput
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

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
    private var thumbnailExtractor: DashThumbnailExtractor? = null
    private var currentMediaItem: MediaItem? = null
    private var tickerJob: Job? = null
    private var filmstripPrewarmJob: Job? = null
    private var lastFilmstripPrewarmCenterMillis: Long? = null
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
        filmstripPrewarmJob?.cancel()
        thumbnailExtractor?.close()
        thumbnailExtractor = null
        lastFilmstripPrewarmCenterMillis = null
        replaceFrameExtractor(item)
        _state.value = PlaybackEngineState(
            phase = PlaybackPhase.Preparing,
            speed = player.playbackParameters.speed,
            resizeMode = resizeMode,
        )
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

    override fun requestFilmstrip(
        positionsMillis: List<Long>,
    ): Flow<PlaybackFilmstripFrameModel> {
        return flow {
            filmstripPrewarmJob?.cancel()
            frameMutex.withLock {
                positionsMillis.forEach { requestedPosition ->
                    val key = requestedPosition.coerceAtLeast(0L)
                    val bitmap = getOrExtractFilmstripFrame(key)
                    emit(
                        PlaybackFilmstripFrameModel(
                            positionMillis = key,
                            image = bitmap?.asImageBitmap(),
                        ),
                    )
                }
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        prepareThumbnailExtractorIfAvailable()
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
        filmstripPrewarmJob?.cancel()
        scope.cancel()
        thumbnailExtractor?.close()
        thumbnailExtractor = null
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
                    message = PlaybackFailureMessage,
                    isRecoverable = true,
                )
            },
        )
        scheduleFilmstripPrewarmIfNeeded()
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
            .setEffects(listOf(Presentation.createForHeight(FilmstripHeightPx)))
            .build()
    }

    private fun prepareThumbnailExtractorIfAvailable() {
        if (thumbnailExtractor != null || !player.currentTracks.hasImageTrack()) {
            return
        }
        val mediaItem = currentMediaItem ?: return
        val extractor = DashThumbnailExtractor(applicationContext)
        thumbnailExtractor = extractor
        extractor.prepare(mediaItem)
        scheduleFilmstripPrewarmIfNeeded()
    }

    private fun scheduleFilmstripPrewarmIfNeeded() {
        if (player.playbackState != Player.STATE_READY ||
            player.currentTracks.hasImageTrack() && thumbnailExtractor == null ||
            filmstripPrewarmJob?.isActive == true
        ) {
            return
        }
        val durationMillis = player.duration.validTime()
        if (durationMillis <= 0L) {
            return
        }
        val centerMillis = quantizeFilmstripPosition(player.currentPosition, durationMillis)
        val lastCenterMillis = lastFilmstripPrewarmCenterMillis
        if (lastCenterMillis != null &&
            abs(centerMillis - lastCenterMillis) < FilmstripPrewarmRefreshMillis
        ) {
            return
        }
        filmstripPrewarmJob = scope.launch {
            val completed = frameMutex.withLock {
                for (positionMillis in filmstripPrewarmPositions(centerMillis, durationMillis)) {
                    getOrExtractFilmstripFrame(positionMillis) ?: return@withLock false
                }
                true
            }
            if (completed) {
                lastFilmstripPrewarmCenterMillis = centerMillis
            }
        }
    }

    private suspend fun getOrExtractFilmstripFrame(positionMillis: Long): Bitmap? {
        frameCache.get(positionMillis)?.let { cached -> return cached }
        val activeThumbnailExtractor = thumbnailExtractor
        val bitmap = if (activeThumbnailExtractor != null) {
            activeThumbnailExtractor.getFrame(positionMillis)
        } else {
            val activeFrameExtractor = frameExtractor ?: return null
            try {
                activeFrameExtractor.getFrame(positionMillis).await().bitmap
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(LogTag, "Filmstrip extraction failed at ${positionMillis}ms", exception)
                null
            }
        }
        bitmap?.let { extracted -> frameCache.put(positionMillis, extracted) }
        return bitmap
    }

    private fun Tracks.hasImageTrack(): Boolean {
        return groups.any { group ->
            group.type == C.TRACK_TYPE_IMAGE && (0 until group.length).any(group::isTrackSupported)
        }
    }

    private fun quantizeFilmstripPosition(positionMillis: Long, durationMillis: Long): Long {
        val bucket = (positionMillis + FilmstripSpacingMillis / 2L) / FilmstripSpacingMillis
        return (bucket * FilmstripSpacingMillis).coerceIn(0L, durationMillis)
    }

    private fun filmstripPrewarmPositions(centerMillis: Long, durationMillis: Long): List<Long> {
        return buildList {
            add(centerMillis)
            for (offset in 1L..FilmstripPrewarmRadiusBuckets) {
                add((centerMillis - offset * FilmstripSpacingMillis).coerceAtLeast(0L))
                add((centerMillis + offset * FilmstripSpacingMillis).coerceAtMost(durationMillis))
            }
        }.distinct()
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
        const val FilmstripSpacingMillis = 5_000L
        const val FilmstripPrewarmRadiusBuckets = 6L
        const val FilmstripPrewarmRefreshMillis = 30_000L
        const val FrameCacheBytes = 8 * 1024 * 1024
        const val LogTag = "Media3PlaybackSession"
        const val PlaybackFailureMessage = "Playback failed."
    }
}

@OptIn(UnstableApi::class)
private class DashThumbnailExtractor(
    context: Context,
) : Player.Listener, ImageOutput {

    private val applicationContext = context.applicationContext
    private val trackAvailability = MutableStateFlow(false)
    private val images = Channel<Bitmap>(Channel.CONFLATED)
    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(FilmstripMediaCache.get(applicationContext))
        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(applicationContext))
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    private val player = ExoPlayer.Builder(applicationContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .setSeekParameters(SeekParameters.CLOSEST_SYNC)
        .build()
        .apply {
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setPrioritizeImageOverVideoEnabled(true)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                .build()
            setImageOutput(this@DashThumbnailExtractor)
            setScrubbingModeEnabled(true)
            addListener(this@DashThumbnailExtractor)
        }

    fun prepare(mediaItem: MediaItem) {
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    suspend fun getFrame(positionMillis: Long): Bitmap? {
        val hasImageTrack = withTimeoutOrNull(PrepareTimeoutMillis) {
            trackAvailability.filter { available -> available }.first()
        } != null
        if (!hasImageTrack) {
            return null
        }
        while (images.tryReceive().isSuccess) {
            // Discard output from the previous seek before awaiting this position.
        }
        player.seekTo(positionMillis.coerceAtLeast(0L))
        return withTimeoutOrNull(FrameTimeoutMillis) { images.receive() }
    }

    override fun onTracksChanged(tracks: Tracks) {
        trackAvailability.value = tracks.groups.any { group ->
            group.type == C.TRACK_TYPE_IMAGE && (0 until group.length).any(group::isTrackSupported)
        }
    }

    override fun onImageAvailable(presentationTimeUs: Long, bitmap: Bitmap) {
        images.trySend(bitmap)
    }

    override fun onDisabled() {
        // No renderer-owned resources.
    }

    fun close() {
        images.close()
        player.removeListener(this)
        player.release()
    }

    private companion object {
        const val PrepareTimeoutMillis = 2_000L
        const val FrameTimeoutMillis = 1_000L
    }
}

@SuppressLint("UnsafeOptInUsageError")
private object FilmstripMediaCache {
    @Volatile
    private var instance: SimpleCache? = null

    private val lock = Any()

    fun get(context: Context): SimpleCache {
        instance?.let { cache -> return cache }
        return synchronized(lock) {
            instance ?: SimpleCache(
                context.cacheDir.resolve(CacheDirectoryName),
                LeastRecentlyUsedCacheEvictor(CacheBytes),
                StandaloneDatabaseProvider(context),
            ).also { cache -> instance = cache }
        }
    }

    private const val CacheDirectoryName = "media3-filmstrip"
    private const val CacheBytes = 16L * 1024L * 1024L
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
