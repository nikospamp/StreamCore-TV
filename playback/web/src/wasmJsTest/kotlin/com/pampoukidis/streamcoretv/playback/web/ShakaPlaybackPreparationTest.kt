@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.pampoukidis.streamcoretv.playback.web

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ShakaPlaybackPreparationTest {
    @Test
    fun visibleLoadRequestsAutoplayAtTheResumePosition(): TestResult {
        return runTest { verifyScenario("autoplay") }
    }

    @Test
    fun pauseWhileUnloadIsPendingSuppressesAutoplay(): TestResult {
        return runTest { verifyScenario("pause-during-unload") }
    }

    @Test
    fun pauseWhileShakaLoadIsPendingSuppressesAutoplay(): TestResult {
        return runTest { verifyScenario("pause-during-load") }
    }

    @Test
    fun explicitPlayDuringLoadRestoresAutoplay(): TestResult {
        return runTest { verifyScenario("play-during-load") }
    }

    @Test
    fun closeDuringLoadRejectsLateCompletionAndDestroysOnce(): TestResult {
        return runTest { verifyScenario("close-during-load") }
    }

    @Test
    fun replacedLoadCannotPlayThePausedReplacement(): TestResult {
        return runTest { verifyScenario("replace-during-load") }
    }

    @Test
    fun replacedLoadFailureCannotFailTheReplacement(): TestResult {
        return runTest { verifyScenario("replace-rejected-load") }
    }

    @Test
    fun browserAutoplayRejectionLeavesReadyControlsWithoutAMediaFailure(): TestResult {
        return runTest { verifyScenario("autoplay-rejected") }
    }

    @Test
    fun oldExplicitPlayRejectionCannotOverwriteReplacementPreparation(): TestResult {
        return runTest { verifyScenario("stale-play-rejected") }
    }

    private suspend fun verifyScenario(scenario: String) {
        val probeId = ShakaPreparationFixture.start(scenario)
        try {
            withTimeout(1_000L) {
                while (!ShakaPreparationFixture.isComplete(probeId)) {
                    delay(1L)
                }
            }
            assertEquals("", ShakaPreparationFixture.failure(probeId))
        } finally {
            ShakaPreparationFixture.release(probeId)
        }
    }
}

@JsModule("./shaka-playback-preparation-test.mjs")
private external object ShakaPreparationFixture {
    fun start(scenario: String): Int
    fun isComplete(id: Int): Boolean
    fun failure(id: Int): String
    fun release(id: Int)
}
