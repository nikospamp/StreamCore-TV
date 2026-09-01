@file:Suppress("UnusedReceiverParameter")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.pampoukidis.streamcoretv.web.shaka

import org.w3c.dom.HTMLVideoElement

@JsModule("./shaka-adapter.mjs")
private external object ShakaAdapter {
    fun createAndDestroyPlayer(videoElement: HTMLVideoElement): Boolean
}

fun runShakaCreateDestroyProbe(videoElement: HTMLVideoElement): Boolean {
    return ShakaAdapter.createAndDestroyPlayer(videoElement)
}
