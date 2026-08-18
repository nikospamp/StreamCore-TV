package com.pampoukidis.streamcoretv.feature.player.mobile.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MobilePlayerResizePolicyTest {

    @Test
    fun `fit never scales the video surface`() {
        assertEquals(
            1f,
            MobilePlayerResizePolicy.scale(
                mode = PlaybackResizeMode.Fit,
                videoAspectRatio = FourByThreeAspectRatio,
                containerAspectRatio = SixteenByNineAspectRatio,
            ),
        )
    }

    @Test
    fun `fill crops narrow video to the container aspect ratio`() {
        assertEquals(
            4f / 3f,
            MobilePlayerResizePolicy.scale(
                mode = PlaybackResizeMode.Fill,
                videoAspectRatio = FourByThreeAspectRatio,
                containerAspectRatio = SixteenByNineAspectRatio,
            ),
            FloatTolerance,
        )
    }

    @Test
    fun `fill leaves matching aspect ratios unchanged`() {
        assertEquals(
            1f,
            MobilePlayerResizePolicy.scale(
                mode = PlaybackResizeMode.Fill,
                videoAspectRatio = SixteenByNineAspectRatio,
                containerAspectRatio = SixteenByNineAspectRatio,
            ),
        )
    }

    private companion object {
        const val FourByThreeAspectRatio = 4f / 3f
        const val SixteenByNineAspectRatio = 16f / 9f
        const val FloatTolerance = 0.0001f
    }
}
