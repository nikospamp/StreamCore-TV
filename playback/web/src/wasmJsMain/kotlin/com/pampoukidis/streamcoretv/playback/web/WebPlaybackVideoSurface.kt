package com.pampoukidis.streamcoretv.playback.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement

internal class WebPlaybackVideoSurface(
    internal val videoElement: HTMLVideoElement,
    private val animationFrames: WebAnimationFrameScheduler = BrowserWebAnimationFrameScheduler,
) : PlaybackVideoSurface {
    private var pendingPointerTransparencyFrameId: Int? = null
    private var released: Boolean = false

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Render(modifier: Modifier) {
        HtmlElementView(
            factory = { videoElement },
            update = { element ->
                element.setAttribute("aria-label", "Video playback")
                applyPointerTransparency(element)
            },
            onRelease = { element -> release(element) },
            modifier = modifier,
        )
    }

    internal fun release(element: HTMLVideoElement) {
        if (element !== videoElement) {
            return
        }
        if (released) {
            return
        }
        released = true
        pendingPointerTransparencyFrameId?.let { requestId -> animationFrames.cancel(requestId) }
        pendingPointerTransparencyFrameId = null
        element.pause()
        element.remove()
    }

    internal fun applyPointerTransparency(element: HTMLVideoElement) {
        if (released || element !== videoElement) {
            return
        }
        pendingPointerTransparencyFrameId?.let { requestId -> animationFrames.cancel(requestId) }
        pendingPointerTransparencyFrameId = null
        setVideoPointerTransparency(element)
        val hostWasApplied = setHostPointerTransparency(element)
        schedulePointerTransparencyRetry(
            element = element,
            hostWasApplied = hostWasApplied,
            remainingUnattachedRetries = MaximumUnattachedRetryFrames,
        )
    }

    private fun schedulePointerTransparencyRetry(
        element: HTMLVideoElement,
        hostWasApplied: Boolean,
        remainingUnattachedRetries: Int,
    ) {
        pendingPointerTransparencyFrameId = animationFrames.request {
            pendingPointerTransparencyFrameId = null
            if (!released) {
                setVideoPointerTransparency(element)
                val hostIsApplied = setHostPointerTransparency(element)
                when {
                    hostIsApplied && hostWasApplied -> Unit
                    hostIsApplied -> schedulePointerTransparencyRetry(
                        element = element,
                        hostWasApplied = true,
                        remainingUnattachedRetries = 0,
                    )

                    remainingUnattachedRetries > 1 -> schedulePointerTransparencyRetry(
                        element = element,
                        hostWasApplied = false,
                        remainingUnattachedRetries = remainingUnattachedRetries - 1,
                    )
                }
            }
        }
    }

    private fun setVideoPointerTransparency(element: HTMLVideoElement) {
        element.style.setProperty(PointerEventsProperty, PointerEventsNone)
    }

    private fun setHostPointerTransparency(element: HTMLVideoElement): Boolean {
        val host = element.parentElement as? HTMLElement ?: return false
        host.style.setProperty(PointerEventsProperty, PointerEventsNone)
        return true
    }

    private companion object {
        const val PointerEventsProperty = "pointer-events"
        const val PointerEventsNone = "none"
        const val MaximumUnattachedRetryFrames = 6
    }
}
