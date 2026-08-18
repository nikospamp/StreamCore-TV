package com.pampoukidis.streamcoretv.feature.player.mobile.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePipPolicyTest {

    @Test
    fun `api 26 through 30 use explicit entry without auto enter`() {
        assertTrue(MobilePipPolicy.supportsExplicitEntry(apiLevel = 26))
        assertTrue(MobilePipPolicy.supportsExplicitEntry(apiLevel = 30))
        assertFalse(MobilePipPolicy.shouldEnableAutoEnter(apiLevel = 30, isPlaying = true))
    }

    @Test
    fun `api 31 auto enters only during active playback`() {
        assertTrue(MobilePipPolicy.shouldEnableAutoEnter(apiLevel = 31, isPlaying = true))
        assertFalse(MobilePipPolicy.shouldEnableAutoEnter(apiLevel = 31, isPlaying = false))
    }

    @Test
    fun `background transition remains foreground only inside pip`() {
        assertTrue(MobilePipPolicy.isForegroundOnStop(isInPictureInPicture = true))
        assertFalse(MobilePipPolicy.isForegroundOnStop(isInPictureInPicture = false))
    }
}
