package com.pampoukidis.streamcoretv.playback.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface PlaybackVideoSurface {
    @Composable
    fun Render(modifier: Modifier)
}