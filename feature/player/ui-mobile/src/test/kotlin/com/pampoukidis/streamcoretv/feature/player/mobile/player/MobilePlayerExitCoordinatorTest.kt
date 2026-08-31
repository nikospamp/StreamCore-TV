package com.pampoukidis.streamcoretv.feature.player.mobile.player

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePlayerExitCoordinatorTest {
    @Test
    fun restoreOrientation_isIdempotent() {
        val requests = mutableListOf<Int>()
        val session = MobilePlayerWindowSession(
            sourceConfigurationOrientation = Configuration.ORIENTATION_PORTRAIT,
            previousRequestedOrientation = 7,
            requestOrientation = requests::add,
        )

        assertTrue(session.restoreOrientation())
        assertFalse(session.restoreOrientation())
        assertEquals(listOf(7), requests)
    }

    @Test
    fun explicitExit_disablesPipAndRequestsOrientationBeforeNavigation() {
        val events = mutableListOf<String>()
        val coordinator = coordinator(events)

        assertTrue(coordinator.requestExit())
        assertEquals(listOf("pip", "orientation", "started"), events)

        coordinator.configurationSettled()
        assertEquals(listOf("pip", "orientation", "started", "navigate"), events)
    }

    @Test
    fun duplicateExitAndSettlement_navigateAtMostOnce() {
        val events = mutableListOf<String>()
        val coordinator = coordinator(events)

        assertTrue(coordinator.requestExit())
        assertFalse(coordinator.requestExit())
        coordinator.configurationSettled()
        coordinator.configurationSettled()
        coordinator.timeout()
        coordinator.cancellationFallback()

        assertEquals(1, events.count { event -> event == "navigate" })
    }

    @Test
    fun timeoutAndCancellationFallback_navigateAtMostOnce() {
        val timeoutEvents = mutableListOf<String>()
        coordinator(timeoutEvents).run {
            requestExit()
            timeout()
            cancellationFallback()
        }
        assertEquals(1, timeoutEvents.count { event -> event == "navigate" })

        val cancellationEvents = mutableListOf<String>()
        coordinator(cancellationEvents).run {
            requestExit()
            cancellationFallback()
            timeout()
        }
        assertEquals(1, cancellationEvents.count { event -> event == "navigate" })
    }

    @Test
    fun noConfigurationChange_doesNotRequireSettlementGate() {
        assertFalse(
            requiresOrientationSettlement(
                sourceOrientation = Configuration.ORIENTATION_PORTRAIT,
                currentOrientation = Configuration.ORIENTATION_PORTRAIT,
            ),
        )
        assertFalse(
            requiresOrientationSettlement(
                sourceOrientation = Configuration.ORIENTATION_UNDEFINED,
                currentOrientation = Configuration.ORIENTATION_LANDSCAPE,
            ),
        )
    }

    @Test
    fun mismatchedConfiguration_requiresSettlementGate() {
        assertTrue(
            requiresOrientationSettlement(
                sourceOrientation = Configuration.ORIENTATION_PORTRAIT,
                currentOrientation = Configuration.ORIENTATION_LANDSCAPE,
            ),
        )
    }

    private fun coordinator(events: MutableList<String>): MobilePlayerExitCoordinator {
        return MobilePlayerExitCoordinator(
            disableAutoEnterPip = { events += "pip" },
            restoreOrientation = { events += "orientation" },
            onExitStarted = { events += "started" },
            navigateBack = { events += "navigate" },
        )
    }
}
