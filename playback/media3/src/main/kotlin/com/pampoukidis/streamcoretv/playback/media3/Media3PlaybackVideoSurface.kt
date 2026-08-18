package com.pampoukidis.streamcoretv.playback.media3

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface

internal class Media3PlaybackVideoSurface(
    private val player: Player,
) : PlaybackVideoSurface {
    @OptIn(UnstableApi::class)
    @Composable
    override fun Render(modifier: Modifier) {
        PlayerSurface(
            player = player,
            modifier = modifier,
        )
    }
}