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
    fun surfaceRetainsTheExactSessionOwnedElementAndReleaseIsSafe() {
        val videoElement = document.createElement("video") as HTMLVideoElement
        val host = document.createElement("div") as HTMLElement
        val animationFrames = FakeWebAnimationFrameScheduler()
        val surface = WebPlaybackVideoSurface(videoElement, animationFrames)
        host.appendChild(videoElement)

        assertTrue(surface.videoElement === videoElement)
        surface.applyPointerTransparency(videoElement)
        surface.applyPointerTransparency(videoElement)

        assertEquals(1, animationFrames.cancelCount)

        surface.release(videoElement)
        surface.release(videoElement)

        assertNull(videoElement.parentNode)
        assertEquals(2, animationFrames.cancelCount)
        assertEquals(0, animationFrames.executedCount)
    }

    @Test
    fun nextFrameOverridesAnAttachedHostPointerEventsAutoValue() {
        val videoElement = document.createElement("video") as HTMLVideoElement
        val host = document.createElement("div") as HTMLElement
        val animationFrames = FakeWebAnimationFrameScheduler()
        val surface = WebPlaybackVideoSurface(videoElement, animationFrames)

        host.style.setProperty("pointer-events", "auto")
        host.appendChild(videoElement)
        surface.applyPointerTransparency(videoElement)

        assertEquals("none", videoElement.style.getPropertyValue("pointer-events"))
        assertEquals("none", host.style.getPropertyValue("pointer-events"))

        host.style.setProperty("pointer-events", "auto")
        animationFrames.runNext()

        assertEquals("none", videoElement.style.getPropertyValue("pointer-events"))
        assertEquals("none", host.style.getPropertyValue("pointer-events"))

        surface.release(videoElement)
        assertNull(videoElement.parentNode)
    }

    @Test
    fun attachmentRetriesUntilHostExistsAndThenReappliesOnTheNextFrame() {
        val videoElement = document.createElement("video") as HTMLVideoElement
        val host = document.createElement("div") as HTMLElement
        val animationFrames = FakeWebAnimationFrameScheduler()
        val surface = WebPlaybackVideoSurface(videoElement, animationFrames)

        surface.applyPointerTransparency(videoElement)
        animationFrames.runNext()

        assertNull(videoElement.parentElement)
        assertEquals(1, animationFrames.pendingCount)

        host.style.setProperty("pointer-events", "auto")
        host.appendChild(videoElement)
        animationFrames.runNext()

        assertEquals("none", host.style.getPropertyValue("pointer-events"))
        assertEquals(1, animationFrames.pendingCount)

        host.style.setProperty("pointer-events", "auto")
        animationFrames.runNext()

        assertEquals("none", host.style.getPropertyValue("pointer-events"))
        assertEquals(0, animationFrames.pendingCount)
        surface.release(videoElement)
    }

    private class FakeWebAnimationFrameScheduler : WebAnimationFrameScheduler {
        private var nextRequestId: Int = 1
        private val callbacks = mutableMapOf<Int, () -> Unit>()
        var cancelCount: Int = 0
            private set
        var executedCount: Int = 0
            private set
        val pendingCount: Int
            get() {
                return callbacks.size
            }

        override fun request(onFrame: () -> Unit): Int {
            val requestId = nextRequestId
            nextRequestId += 1
            callbacks[requestId] = onFrame
            return requestId
        }

        override fun cancel(requestId: Int) {
            if (callbacks.remove(requestId) != null) {
                cancelCount += 1
            }
        }

        fun runNext() {
            val requestId = callbacks.keys.firstOrNull() ?: return
            val callback = callbacks.remove(requestId) ?: return
            executedCount += 1
            callback()
        }
    }
}
