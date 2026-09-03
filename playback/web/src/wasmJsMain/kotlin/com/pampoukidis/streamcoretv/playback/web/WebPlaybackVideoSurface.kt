package com.pampoukidis.streamcoretv.playback.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface

internal class WebPlaybackVideoSurface : PlaybackVideoSurface {
    @Composable
    override fun Render(modifier: Modifier) {
        Layout(
            content = {},
            modifier = modifier,
        ) { _, constraints ->
            layout(
                width = constraints.minWidth,
                height = constraints.minHeight,
            ) {}
        }
    }
}
