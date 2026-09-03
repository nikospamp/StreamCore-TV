package com.pampoukidis.streamcoretv.playback.web

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ShakaPlaybackAdapterTest {
    @Test
    fun adapterDerivesActiveVariantIdsAndControlsTextVisibility() {
        assertTrue(verifyShakaTrackAndTextAdapterContract())
    }

    @Test
    fun videoAutoReenablesAbrWithoutClearingTheAudioOverride() {
        assertTrue(verifyShakaVideoAutoWithAudioOverrideAdapterContract())
    }

    @Test
    fun closeStartsExactlyOneDestroyAndObservesResolution() {
        runTest {
            val probeId = runShakaCleanupProbe(rejectDestroy = false)
            try {
                awaitCleanupSettlement(probeId)

                assertEquals(1, shakaCleanupProbeDestroyStartCount(probeId))
                assertTrue(isShakaCleanupProbeSettled(probeId))
                assertFalse(didShakaCleanupProbeCatchRejection(probeId))
            } finally {
                releaseShakaCleanupProbe(probeId)
            }
        }
    }

    @Test
    fun closeCatchesDestroyRejectionWithoutStartingTwice() {
        runTest {
            val probeId = runShakaCleanupProbe(rejectDestroy = true)
            try {
                awaitCleanupSettlement(probeId)

                assertEquals(1, shakaCleanupProbeDestroyStartCount(probeId))
                assertTrue(isShakaCleanupProbeSettled(probeId))
                assertTrue(didShakaCleanupProbeCatchRejection(probeId))
            } finally {
                releaseShakaCleanupProbe(probeId)
            }
        }
    }

    private suspend fun awaitCleanupSettlement(probeId: Int) {
        withTimeout(CleanupTimeoutMillis) {
            while (!isShakaCleanupProbeSettled(probeId)) {
                delay(1L)
            }
        }
    }

    private companion object {
        const val CleanupTimeoutMillis = 1_000L
    }
}
