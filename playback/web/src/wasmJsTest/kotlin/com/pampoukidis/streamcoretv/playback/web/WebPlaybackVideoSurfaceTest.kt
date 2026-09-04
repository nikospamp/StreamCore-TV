package com.pampoukidis.streamcoretv.playback.web

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class WebPlaybackVideoSurfaceTest {
    @Test
    fun mountUsesExactSessionElementAndRestoresLayerStyles() {
        val videoElement = document.createElement("video") as HTMLVideoElement
        val videoLayer = document.createElement("div") as HTMLElement
        val surface = WebPlaybackVideoSurface(videoElement)

        videoElement.style.setProperty("pointer-events", "auto")
        videoLayer.style.setProperty("pointer-events", "auto")
        videoLayer.style.setProperty("visibility", "hidden")
        surface.mount(videoLayer)

        assertTrue(surface.videoElement === videoElement)
        assertTrue(videoElement.parentElement === videoLayer)
        assertEquals("none", videoElement.style.getPropertyValue("pointer-events"))
        assertEquals("none", videoLayer.style.getPropertyValue("pointer-events"))
        assertEquals("visible", videoLayer.style.getPropertyValue("visibility"))

        videoElement.style.setProperty("pointer-events", "auto")
        videoLayer.style.setProperty("pointer-events", "auto")
        videoLayer.style.setProperty("visibility", "hidden")
        surface.mount(videoLayer)

        assertEquals(1, videoLayer.childElementCount)
        assertEquals("none", videoElement.style.getPropertyValue("pointer-events"))
        assertEquals("none", videoLayer.style.getPropertyValue("pointer-events"))
        assertEquals("visible", videoLayer.style.getPropertyValue("visibility"))
    }

    @Test
    fun releaseRemovesOnlyOwnedVideoAndHidesLayerOnlyWhenEmpty() {
        val firstVideo = document.createElement("video") as HTMLVideoElement
        val secondVideo = document.createElement("video") as HTMLVideoElement
        val videoLayer = document.createElement("div") as HTMLElement
        val firstSurface = WebPlaybackVideoSurface(firstVideo)
        val secondSurface = WebPlaybackVideoSurface(secondVideo)
        firstSurface.mount(videoLayer)
        secondSurface.mount(videoLayer)

        firstSurface.release(videoLayer)
        firstSurface.release(videoLayer)

        assertNull(firstVideo.parentNode)
        assertTrue(secondVideo.parentElement === videoLayer)
        assertEquals(1, videoLayer.childElementCount)
        assertEquals("visible", videoLayer.style.getPropertyValue("visibility"))

        secondSurface.release(videoLayer)

        assertNull(secondVideo.parentNode)
        assertEquals(0, videoLayer.childElementCount)
        assertEquals("hidden", videoLayer.style.getPropertyValue("visibility"))
    }

    @Test
    fun releasedSurfaceCannotReattachItsSessionElement() {
        val videoElement = document.createElement("video") as HTMLVideoElement
        val videoLayer = document.createElement("div") as HTMLElement
        val surface = WebPlaybackVideoSurface(videoElement)
        surface.mount(videoLayer)
        surface.release(videoLayer)

        surface.mount(videoLayer)

        assertNull(videoElement.parentNode)
        assertEquals(0, videoLayer.childElementCount)
        assertEquals("hidden", videoLayer.style.getPropertyValue("visibility"))
    }
}
