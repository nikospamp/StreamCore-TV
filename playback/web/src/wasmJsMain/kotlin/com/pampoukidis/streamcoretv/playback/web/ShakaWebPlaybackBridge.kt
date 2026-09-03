@file:Suppress("UnusedReceiverParameter")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import kotlinx.browser.window
import org.w3c.dom.HTMLVideoElement
import kotlin.js.JsAny

internal class ShakaWebPlaybackBridge(
    videoElement: HTMLVideoElement,
) : WebPlaybackBridge {
    private val handle: JsAny = ShakaPlaybackAdapter.create(videoElement)
    private var listener: WebPlaybackBridgeListener? = null
    private var closed: Boolean = false
    private var cachedTracks: CachedTracks? = null
    private var nextFilmstripRequestId: Int = 1
    private val filmstripRequests = mutableMapOf<Int, ActiveFilmstripRequest>()
    private var tickerId: Int? = window.setInterval(
        handler = {
            publishSnapshot()
            publishFilmstripResults()
            null
        },
        timeout = PositionUpdateMillis,
    )

    override fun setListener(listener: WebPlaybackBridgeListener?) {
        if (closed) {
            return
        }
        this.listener = listener
        publishSnapshot()
    }

    override fun reset(generation: Int) {
        if (closed) {
            return
        }
        cancelActiveFilmstripRequests()
        ShakaPlaybackAdapter.reset(handle, generation)
        publishSnapshot()
    }

    override fun load(
        generation: Int,
        uri: String,
        mimeType: String?,
        startPositionMillis: Long,
    ) {
        if (closed) {
            return
        }
        cancelActiveFilmstripRequests()
        ShakaPlaybackAdapter.load(
            handle = handle,
            generation = generation,
            uri = uri,
            mimeType = mimeType.orEmpty(),
            startSeconds = startPositionMillis.coerceAtLeast(0L).toDouble() / MillisPerSecond,
        )
        publishSnapshot()
    }

    override fun play() {
        mutate { activeHandle -> ShakaPlaybackAdapter.play(activeHandle) }
    }

    override fun pause() {
        mutate { activeHandle -> ShakaPlaybackAdapter.pause(activeHandle) }
    }

    override fun seekTo(positionMillis: Long) {
        if (closed) {
            return
        }
        ShakaPlaybackAdapter.seekTo(
            handle = handle,
            positionSeconds = positionMillis.coerceAtLeast(0L).toDouble() / MillisPerSecond,
        )
        publishSnapshot()
    }

    override fun setSpeed(speed: Float) {
        if (closed) {
            return
        }
        ShakaPlaybackAdapter.setSpeed(handle, speed.toDouble())
        publishSnapshot()
    }

    override fun selectVideoTrack(trackId: String?) {
        if (closed) {
            return
        }
        ShakaPlaybackAdapter.selectVideoTrack(handle, trackId.orEmpty())
        publishSnapshot()
    }

    override fun selectAudioTrack(trackId: String?) {
        if (closed) {
            return
        }
        ShakaPlaybackAdapter.selectAudioTrack(handle, trackId.orEmpty())
        publishSnapshot()
    }

    override fun selectTextTrack(trackId: String?) {
        if (closed) {
            return
        }
        ShakaPlaybackAdapter.selectTextTrack(handle, trackId.orEmpty())
        publishSnapshot()
    }

    override fun setResizeMode(mode: PlaybackResizeMode) {
        if (closed) {
            return
        }
        ShakaPlaybackAdapter.setResizeMode(
            handle,
            when (mode) {
                PlaybackResizeMode.Fit -> "fit"
                PlaybackResizeMode.Fill -> "fill"
            },
        )
        publishSnapshot()
    }

    override fun requestFilmstrip(
        generation: Int,
        positionsMillis: List<Long>,
        listener: WebFilmstripListener,
    ): Int? {
        if (closed) {
            return null
        }
        val requestId = nextFilmstripRequestId
        nextFilmstripRequestId = if (requestId == Int.MAX_VALUE) 1 else requestId + 1
        filmstripRequests[requestId] = ActiveFilmstripRequest(listener = listener)
        ShakaPlaybackAdapter.requestFilmstrip(
            handle = handle,
            generation = generation,
            requestId = requestId,
            positionsMillisCsv = positionsMillis.joinToString(separator = ","),
        )
        publishFilmstripResults()
        return requestId
    }

    override fun cancelFilmstrip(requestId: Int) {
        filmstripRequests.remove(requestId)
        ShakaPlaybackAdapter.cancelFilmstrip(handle, requestId)
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        listener = null
        cachedTracks = null
        cancelActiveFilmstripRequests()
        tickerId?.let { activeTickerId -> window.clearInterval(activeTickerId) }
        tickerId = null
        ShakaPlaybackAdapter.close(handle)
    }

    private fun publishFilmstripResults() {
        if (closed) {
            return
        }
        filmstripRequests.toMap().forEach { (requestId, activeRequest) ->
            val resultCount = ShakaPlaybackAdapter.filmstripResultCount(handle, requestId)
            while (
                activeRequest.deliveredCount < resultCount &&
                filmstripRequests[requestId] === activeRequest
            ) {
                val index = activeRequest.deliveredCount
                activeRequest.deliveredCount += 1
                val encodedPng = ShakaPlaybackAdapter.filmstripResultEncodedPng(handle, requestId, index)
                if (encodedPng.isNotBlank()) {
                    runCatching {
                        activeRequest.listener.onFrame(
                            WebFilmstripFrameSnapshot(
                                positionMillis = ShakaPlaybackAdapter
                                    .filmstripResultPositionMillis(handle, requestId, index)
                                    .toSafeMillis(),
                                encodedPng = encodedPng,
                            ),
                        )
                    }
                }
            }
            if (filmstripRequests[requestId] !== activeRequest) {
                return@forEach
            }
            if (ShakaPlaybackAdapter.filmstripIsComplete(handle, requestId)) {
                filmstripRequests.remove(requestId)
                ShakaPlaybackAdapter.releaseFilmstrip(handle, requestId)
                runCatching { activeRequest.listener.onComplete() }
            }
        }
    }

    private fun cancelActiveFilmstripRequests() {
        val activeRequests = filmstripRequests.toMap()
        filmstripRequests.clear()
        activeRequests.forEach { (requestId, activeRequest) ->
            ShakaPlaybackAdapter.cancelFilmstrip(handle, requestId)
            runCatching { activeRequest.listener.onComplete() }
        }
    }

    private fun mutate(block: (JsAny) -> Unit) {
        if (closed) {
            return
        }
        block(handle)
        publishSnapshot()
    }

    private fun publishSnapshot() {
        if (closed) {
            return
        }
        val tracks = currentTracks()
        listener?.onSnapshot(
            generation = ShakaPlaybackAdapter.generation(handle),
            snapshot = WebPlaybackSnapshot(
                phase = ShakaPlaybackAdapter.phase(handle).toWebPlaybackPhase(),
                isPlaying = ShakaPlaybackAdapter.isPlaying(handle),
                positionMillis = ShakaPlaybackAdapter.positionMillis(handle).toSafeMillis(),
                durationMillis = ShakaPlaybackAdapter.durationMillis(handle).toSafeMillis(),
                bufferedPositionMillis = ShakaPlaybackAdapter.bufferedPositionMillis(handle).toSafeMillis(),
                videoAspectRatio = ShakaPlaybackAdapter.aspectRatio(handle)
                    .takeIf { value -> value.isFinite() && value > 0.0 }
                    ?.toFloat(),
                speed = ShakaPlaybackAdapter.speed(handle).toFloat().coerceIn(MinSpeed, MaxSpeed),
                resizeMode = if (ShakaPlaybackAdapter.resizeMode(handle) == "fill") {
                    PlaybackResizeMode.Fill
                } else {
                    PlaybackResizeMode.Fit
                },
                videoTracks = tracks.video,
                audioTracks = tracks.audio,
                textTracks = tracks.text,
                selectedVideoTrackId = selectedTrackId(PlaybackTrackType.Video),
                selectedAudioTrackId = selectedTrackId(PlaybackTrackType.Audio),
                selectedTextTrackId = selectedTrackId(PlaybackTrackType.Text),
                failure = if (ShakaPlaybackAdapter.hasFailure(handle)) {
                    WebPlaybackFailure(
                        isRecoverable = ShakaPlaybackAdapter.isFailureRecoverable(handle),
                    )
                } else {
                    null
                },
            ),
        )
    }

    private fun currentTracks(): CachedTracks {
        val revision = ShakaPlaybackAdapter.tracksRevision(handle)
        cachedTracks?.takeIf { tracks -> tracks.revision == revision }?.let { tracks ->
            return tracks
        }
        return CachedTracks(
            revision = revision,
            video = tracks(PlaybackTrackType.Video),
            audio = tracks(PlaybackTrackType.Audio),
            text = tracks(PlaybackTrackType.Text),
        ).also { tracks -> cachedTracks = tracks }
    }

    private fun tracks(type: PlaybackTrackType): List<WebPlaybackTrackSnapshot> {
        val bridgeType = type.bridgeName()
        return List(ShakaPlaybackAdapter.trackCount(handle, bridgeType).coerceAtLeast(0)) { index ->
            WebPlaybackTrackSnapshot(
                id = ShakaPlaybackAdapter.trackId(handle, bridgeType, index),
                type = type,
                label = ShakaPlaybackAdapter.trackLabel(handle, bridgeType, index),
                language = ShakaPlaybackAdapter.trackLanguage(handle, bridgeType, index)
                    .takeIf(String::isNotBlank),
                videoHeight = ShakaPlaybackAdapter.trackVideoHeight(handle, bridgeType, index)
                    .takeIf { height -> height > 0 },
                isSupported = ShakaPlaybackAdapter.trackIsSupported(handle, bridgeType, index),
            )
        }.filter { track -> track.id.isNotBlank() }
    }

    private fun selectedTrackId(type: PlaybackTrackType): String? {
        return ShakaPlaybackAdapter.selectedTrackId(handle, type.bridgeName())
            .takeIf(String::isNotBlank)
    }

    private companion object {
        const val PositionUpdateMillis = 250
        const val MillisPerSecond = 1_000.0
        const val MinSpeed = 0.5f
        const val MaxSpeed = 2f
    }

    private data class CachedTracks(
        val revision: Int,
        val video: List<WebPlaybackTrackSnapshot>,
        val audio: List<WebPlaybackTrackSnapshot>,
        val text: List<WebPlaybackTrackSnapshot>,
    )

    private data class ActiveFilmstripRequest(
        val listener: WebFilmstripListener,
        var deliveredCount: Int = 0,
    )
}

internal fun runShakaCleanupProbe(rejectDestroy: Boolean): Int {
    return ShakaPlaybackAdapter.runCleanupProbe(rejectDestroy)
}

internal fun shakaCleanupProbeDestroyStartCount(probeId: Int): Int {
    return ShakaPlaybackAdapter.cleanupProbeDestroyStartCount(probeId)
}

internal fun isShakaCleanupProbeSettled(probeId: Int): Boolean {
    return ShakaPlaybackAdapter.cleanupProbeDestroySettled(probeId)
}

internal fun didShakaCleanupProbeCatchRejection(probeId: Int): Boolean {
    return ShakaPlaybackAdapter.cleanupProbeRejectionCaught(probeId)
}

internal fun releaseShakaCleanupProbe(probeId: Int) {
    ShakaPlaybackAdapter.releaseCleanupProbe(probeId)
}

internal fun verifyShakaTrackAndTextAdapterContract(): Boolean {
    return ShakaPlaybackAdapter.verifyTrackAndTextContract()
}

internal fun verifyShakaVideoAutoWithAudioOverrideAdapterContract(): Boolean {
    return ShakaPlaybackAdapter.verifyVideoAutoWithAudioOverrideContract()
}

private fun String.toWebPlaybackPhase(): WebPlaybackPhase {
    return when (this) {
        "preparing" -> WebPlaybackPhase.Preparing
        "buffering" -> WebPlaybackPhase.Buffering
        "ready" -> WebPlaybackPhase.Ready
        "ended" -> WebPlaybackPhase.Ended
        "error" -> WebPlaybackPhase.Error
        else -> WebPlaybackPhase.Idle
    }
}

private fun PlaybackTrackType.bridgeName(): String {
    return when (this) {
        PlaybackTrackType.Video -> "video"
        PlaybackTrackType.Audio -> "audio"
        PlaybackTrackType.Text -> "text"
    }
}

private fun Double.toSafeMillis(): Long {
    return if (isFinite() && this > 0.0) toLong() else 0L
}

@JsModule("./shaka-playback-adapter.mjs")
private external object ShakaPlaybackAdapter {
    fun create(videoElement: HTMLVideoElement): JsAny
    fun reset(handle: JsAny, generation: Int)
    fun load(handle: JsAny, generation: Int, uri: String, mimeType: String, startSeconds: Double)
    fun play(handle: JsAny)
    fun pause(handle: JsAny)
    fun seekTo(handle: JsAny, positionSeconds: Double)
    fun setSpeed(handle: JsAny, speed: Double)
    fun selectVideoTrack(handle: JsAny, trackId: String)
    fun selectAudioTrack(handle: JsAny, trackId: String)
    fun selectTextTrack(handle: JsAny, trackId: String)
    fun setResizeMode(handle: JsAny, resizeMode: String)
    fun requestFilmstrip(
        handle: JsAny,
        generation: Int,
        requestId: Int,
        positionsMillisCsv: String,
    )
    fun cancelFilmstrip(handle: JsAny, requestId: Int)
    fun filmstripResultCount(handle: JsAny, requestId: Int): Int
    fun filmstripResultPositionMillis(handle: JsAny, requestId: Int, index: Int): Double
    fun filmstripResultEncodedPng(handle: JsAny, requestId: Int, index: Int): String
    fun filmstripIsComplete(handle: JsAny, requestId: Int): Boolean
    fun releaseFilmstrip(handle: JsAny, requestId: Int)
    fun close(handle: JsAny)
    fun generation(handle: JsAny): Int
    fun phase(handle: JsAny): String
    fun isPlaying(handle: JsAny): Boolean
    fun positionMillis(handle: JsAny): Double
    fun durationMillis(handle: JsAny): Double
    fun bufferedPositionMillis(handle: JsAny): Double
    fun aspectRatio(handle: JsAny): Double
    fun speed(handle: JsAny): Double
    fun resizeMode(handle: JsAny): String
    fun hasFailure(handle: JsAny): Boolean
    fun isFailureRecoverable(handle: JsAny): Boolean
    fun tracksRevision(handle: JsAny): Int
    fun trackCount(handle: JsAny, type: String): Int
    fun trackId(handle: JsAny, type: String, index: Int): String
    fun trackLabel(handle: JsAny, type: String, index: Int): String
    fun trackLanguage(handle: JsAny, type: String, index: Int): String
    fun trackVideoHeight(handle: JsAny, type: String, index: Int): Int
    fun trackIsSupported(handle: JsAny, type: String, index: Int): Boolean
    fun selectedTrackId(handle: JsAny, type: String): String
    fun runCleanupProbe(rejectDestroy: Boolean): Int
    fun cleanupProbeDestroyStartCount(probeId: Int): Int
    fun cleanupProbeDestroySettled(probeId: Int): Boolean
    fun cleanupProbeRejectionCaught(probeId: Int): Boolean
    fun releaseCleanupProbe(probeId: Int)
    fun verifyTrackAndTextContract(): Boolean
    fun verifyVideoAutoWithAudioOverrideContract(): Boolean
}
