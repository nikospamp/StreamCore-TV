package com.pampoukidis.streamcoretv.feature.player.tv.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TvPlayerResizePolicyTest {
    @Test
    fun fitNeverScalesPastContainer() {
        assertEquals(
            1f,
            TvPlayerResizePolicy.scale(
                mode = PlaybackResizeMode.Fit,
                videoAspectRatio = 4f / 3f,
                containerAspectRatio = 16f / 9f,
            ),
        )
    }

    @Test
    fun fillScalesToCoverContainer() {
        assertEquals(
            4f / 3f,
            TvPlayerResizePolicy.scale(
                mode = PlaybackResizeMode.Fill,
                videoAspectRatio = 4f / 3f,
                containerAspectRatio = 16f / 9f,
            ),
            0.001f,
        )
    }
}
