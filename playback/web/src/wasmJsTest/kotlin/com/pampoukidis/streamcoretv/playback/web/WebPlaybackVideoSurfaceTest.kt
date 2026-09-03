package com.pampoukidis.streamcoretv.playback.web

import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class WebPlaybackVideoSurfaceTest {
    @Test
    fun surfaceRetainsTheExactSessionOwnedElementAndReleaseIsSafe() {
        val videoElement = document.createElement("video") as HTMLVideoElement
        val surface = WebPlaybackVideoSurface(videoElement)
        document.body?.appendChild(videoElement)

        assertTrue(surface.videoElement === videoElement)

        surface.release(videoElement)
        surface.release(videoElement)

        assertNull(videoElement.parentNode)
    }
}
