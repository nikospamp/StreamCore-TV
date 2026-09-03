package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode

internal class FakeWebPlaybackBridge : WebPlaybackBridge {
    var listener: WebPlaybackBridgeListener? = null
        private set
    var retainedListener: WebPlaybackBridgeListener? = null
        private set
    var listenerAttachCount: Int = 0
        private set
    var listenerDetachCount: Int = 0
        private set
    var closeCount: Int = 0
        private set
    var loadCount: Int = 0
        private set
    var resetCount: Int = 0
        private set
    var lastResetGeneration: Int? = null
        private set
    var lastLoadGeneration: Int? = null
        private set
    var lastLoadUri: String? = null
        private set
    var lastLoadMimeType: String? = null
        private set
    var lastLoadStartPositionMillis: Long? = null
        private set
    var playCount: Int = 0
        private set
    var pauseCount: Int = 0
        private set
    val seekPositionsMillis: MutableList<Long> = mutableListOf()
    val speeds: MutableList<Float> = mutableListOf()
    val videoTrackSelections: MutableList<String?> = mutableListOf()
    val audioTrackSelections: MutableList<String?> = mutableListOf()
    val textTrackSelections: MutableList<String?> = mutableListOf()
    val resizeModes: MutableList<PlaybackResizeMode> = mutableListOf()
    var filmstripFrames: List<WebFilmstripFrameSnapshot> = emptyList()
    var completeFilmstripRequestsImmediately: Boolean = true
    var lastFilmstripGeneration: Int? = null
        private set
    var lastFilmstripPositionsMillis: List<Long> = emptyList()
        private set
    var filmstripCancelCount: Int = 0
        private set
    private var nextFilmstripRequestId: Int = 1
    private val filmstripListeners = mutableMapOf<Int, WebFilmstripListener>()

    override fun setListener(listener: WebPlaybackBridgeListener?) {
        this.listener = listener
        if (listener == null) {
            listenerDetachCount += 1
        } else {
            retainedListener = listener
            listenerAttachCount += 1
        }
    }

    override fun load(
        generation: Int,
        uri: String,
        mimeType: String?,
        startPositionMillis: Long,
    ) {
        loadCount += 1
        lastLoadGeneration = generation
        lastLoadUri = uri
        lastLoadMimeType = mimeType
        lastLoadStartPositionMillis = startPositionMillis
    }

    override fun reset(generation: Int) {
        resetCount += 1
        lastResetGeneration = generation
    }

    override fun play() {
        playCount += 1
    }

    override fun pause() {
        pauseCount += 1
    }

    override fun seekTo(positionMillis: Long) {
        seekPositionsMillis += positionMillis
    }

    override fun setSpeed(speed: Float) {
        speeds += speed
    }

    override fun selectVideoTrack(trackId: String?) {
        videoTrackSelections += trackId
    }

    override fun selectAudioTrack(trackId: String?) {
        audioTrackSelections += trackId
    }

    override fun selectTextTrack(trackId: String?) {
        textTrackSelections += trackId
    }

    override fun setResizeMode(mode: PlaybackResizeMode) {
        resizeModes += mode
    }

    override fun requestFilmstrip(
        generation: Int,
        positionsMillis: List<Long>,
        listener: WebFilmstripListener,
    ): Int {
        val requestId = nextFilmstripRequestId
        nextFilmstripRequestId += 1
        lastFilmstripGeneration = generation
        lastFilmstripPositionsMillis = positionsMillis
        filmstripListeners[requestId] = listener
        if (completeFilmstripRequestsImmediately) {
            filmstripFrames.forEach(listener::onFrame)
            filmstripListeners.remove(requestId)
            listener.onComplete()
        }
        return requestId
    }

    override fun cancelFilmstrip(requestId: Int) {
        filmstripCancelCount += 1
        filmstripListeners.remove(requestId)
    }

    override fun close() {
        closeCount += 1
        val listeners = filmstripListeners.values.toList()
        filmstripListeners.clear()
        listeners.forEach(WebFilmstripListener::onComplete)
    }

    fun emit(generation: Int, snapshot: WebPlaybackSnapshot) {
        listener?.onSnapshot(generation, snapshot)
    }

    fun emitLate(generation: Int, snapshot: WebPlaybackSnapshot) {
        retainedListener?.onSnapshot(generation, snapshot)
    }
}
