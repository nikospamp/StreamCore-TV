package com.pampoukidis.streamcoretv.playback.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import org.w3c.dom.HTMLVideoElement

internal class WebPlaybackVideoSurface(
    internal val videoElement: HTMLVideoElement,
) : PlaybackVideoSurface {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Render(modifier: Modifier) {
        HtmlElementView(
            factory = { videoElement },
            update = { element ->
                element.setAttribute("aria-label", "Video playback")
            },
            onRelease = { element -> release(element) },
            modifier = modifier,
        )
    }

    internal fun release(element: HTMLVideoElement) {
        if (element !== videoElement) {
            return
        }
        element.pause()
        element.remove()
    }
}
