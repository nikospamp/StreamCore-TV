package com.pampoukidis.streamcoretv.playback.web

internal interface WebFilmstripListener {
    fun onFrame(frame: WebFilmstripFrameSnapshot)
    fun onComplete()
}
