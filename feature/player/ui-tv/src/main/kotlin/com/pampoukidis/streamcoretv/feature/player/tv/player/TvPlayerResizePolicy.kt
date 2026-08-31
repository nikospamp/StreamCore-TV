package com.pampoukidis.streamcoretv.feature.player.tv.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import kotlin.math.max

internal object TvPlayerResizePolicy {
    fun scale(
        mode: PlaybackResizeMode,
        videoAspectRatio: Float?,
        containerAspectRatio: Float,
    ): Float {
        if (
            mode != PlaybackResizeMode.Fill ||
            videoAspectRatio == null ||
            videoAspectRatio <= 0f ||
            containerAspectRatio <= 0f
        ) {
            return 1f
        }

        return max(
            videoAspectRatio / containerAspectRatio,
            containerAspectRatio / videoAspectRatio,
        ).coerceAtLeast(1f)
    }
}
