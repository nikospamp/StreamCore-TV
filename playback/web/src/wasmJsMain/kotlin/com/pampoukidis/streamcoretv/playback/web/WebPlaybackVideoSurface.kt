package com.pampoukidis.streamcoretv.playback.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement

internal class WebPlaybackVideoSurface(
    internal val videoElement: HTMLVideoElement,
) : PlaybackVideoSurface {
    private var released: Boolean = false

    @Composable
    override fun Render(modifier: Modifier) {
        DisposableEffect(videoElement) {
            val videoLayer = requireNotNull(
                document.getElementById(WEB_PLAYBACK_VIDEO_LAYER_ID) as? HTMLElement,
            ) {
                "Missing #$WEB_PLAYBACK_VIDEO_LAYER_ID playback video layer."
            }
            mount(videoLayer)
            onDispose {
                release(videoLayer)
            }
        }
        Layout(
            content = {},
            modifier = modifier.drawBehind {
                drawRect(
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear,
                )
            },
        ) { _, constraints ->
            layout(
                width = constraints.maxWidth,
                height = constraints.maxHeight,
            ) {}
        }
    }

    internal fun mount(videoLayer: HTMLElement) {
        if (released) {
            return
        }
        videoElement.setAttribute("aria-label", VideoAriaLabel)
        videoElement.style.setProperty(PointerEventsProperty, PointerEventsNone)
        videoLayer.style.setProperty(PointerEventsProperty, PointerEventsNone)
        videoLayer.style.setProperty(VisibilityProperty, VisibilityVisible)
        if (videoElement.parentElement !== videoLayer) {
            videoLayer.appendChild(videoElement)
        }
    }

    internal fun release(videoLayer: HTMLElement) {
        if (released) {
            return
        }
        released = true
        videoElement.pause()
        videoElement.remove()
        hideLayerWhenEmpty(videoLayer)
    }

    private fun hideLayerWhenEmpty(videoLayer: HTMLElement) {
        if (videoLayer.childElementCount == 0) {
            videoLayer.style.setProperty(VisibilityProperty, VisibilityHidden)
        }
    }

    private companion object {
        const val VideoAriaLabel = "Video playback"
        const val PointerEventsProperty = "pointer-events"
        const val PointerEventsNone = "none"
        const val VisibilityProperty = "visibility"
        const val VisibilityVisible = "visible"
        const val VisibilityHidden = "hidden"
    }
}
