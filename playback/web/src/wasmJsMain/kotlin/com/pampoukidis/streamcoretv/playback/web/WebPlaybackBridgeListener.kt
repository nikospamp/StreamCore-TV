package com.pampoukidis.streamcoretv.playback.web

internal fun interface WebPlaybackBridgeListener {
    fun onSnapshot(generation: Int, snapshot: WebPlaybackSnapshot)
}
