package com.pampoukidis.streamcoretv.playback.web

import kotlinx.browser.window

internal interface WebAnimationFrameScheduler {
    fun request(onFrame: () -> Unit): Int
    fun cancel(requestId: Int)
}

internal object BrowserWebAnimationFrameScheduler : WebAnimationFrameScheduler {
    override fun request(onFrame: () -> Unit): Int {
        return window.requestAnimationFrame { onFrame() }
    }

    override fun cancel(requestId: Int) {
        window.cancelAnimationFrame(requestId)
    }
}
