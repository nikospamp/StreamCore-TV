package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement

internal class WebPlaybackSessionFactory : PlaybackSessionFactory {
    override fun create(): PlaybackSession {
        val videoElement = document.createElement("video") as HTMLVideoElement
        return WebPlaybackSession(
            bridge = ShakaWebPlaybackBridge(videoElement),
            videoSurface = WebPlaybackVideoSurface(videoElement),
        )
    }
}
