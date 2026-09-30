package com.pampoukidis.streamcoretv.web.navigation

import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalWasmJsInterop::class)
class WebProfilePinHistoryTest {
    @Test
    fun browserBackCancelsOnceAndKeepsTheChooserRoute(): TestResult {
        return runTest {
            val controller = WebNavigationController()
            try {
                controller.replace(WebRoute.Profiles)
                var cancellations = 0
                controller.setProfilePinBackHandler(handler = { cancellations++ })
                assertEquals(WebRoute.Profiles.path, window.location.pathname)
                assertEquals("streamcore-profile-pin", window.history.state?.toString())
                // Recomposition can replace the callback without creating another history entry.
                controller.setProfilePinBackHandler(handler = { cancellations++ })
                awaitPinHistoryChange { window.history.back() }
                assertEquals(1, cancellations)
                assertEquals(WebRoute.Profiles, controller.route.value)
                assertNull(window.history.state)
                controller.setProfilePinBackHandler(null)
                assertEquals(1, cancellations)
            } finally {
                controller.replace(WebRoute.Root)
                controller.close()
            }
        }
    }

    @Test
    fun uiCancellationRemovesTheOwnedEntryWithoutCallingCancelAgain(): TestResult {
        return runTest {
            val controller = WebNavigationController()
            try {
                controller.replace(WebRoute.Profiles)
                var cancellations = 0
                controller.setProfilePinBackHandler(handler = { cancellations++ })
                awaitPinHistoryChange { controller.setProfilePinBackHandler(null) }
                assertEquals(0, cancellations)
                assertEquals(WebRoute.Profiles, controller.route.value)
                assertEquals(WebRoute.Profiles.path, window.location.pathname)
                assertNull(window.history.state)
            } finally {
                controller.replace(WebRoute.Root)
                controller.close()
            }
        }
    }

    @Test
    fun activationBeforeNavigationKeepsThePinEntryForReplacement(): TestResult {
        return runTest {
            val controller = WebNavigationController()
            try {
                controller.replace(WebRoute.Profiles)
                var cancellations = 0
                controller.setProfilePinBackHandler(handler = { cancellations++ })
                // The ViewModel clears PIN state before sending ProfileSelected.
                controller.setProfilePinBackHandler(null, activated = true)
                controller.navigate(WebRoute.Home)
                assertEquals(WebRoute.Home, controller.route.value)
                assertEquals(WebRoute.Home.path, window.location.pathname)
                assertNull(window.history.state)
                awaitPinHistoryChange { window.history.back() }
                assertEquals(WebRoute.Profiles, controller.route.value)
                assertEquals(0, cancellations)
                assertNull(window.history.state)
            } finally {
                controller.replace(WebRoute.Root)
                controller.close()
            }
        }
    }

    @Test
    fun successfulNavigationReplacesPinEntryAndDisposalDoesNotGoBack(): TestResult {
        return runTest {
            val controller = WebNavigationController()
            try {
                controller.replace(WebRoute.Profiles)
                var cancellations = 0
                controller.setProfilePinBackHandler(handler = { cancellations++ })
                controller.navigate(WebRoute.Home)
                controller.setProfilePinBackHandler(null)
                assertEquals(WebRoute.Home, controller.route.value)
                assertEquals(WebRoute.Home.path, window.location.pathname)
                assertNull(window.history.state)
                assertEquals(0, cancellations)
                awaitPinHistoryChange { window.history.back() }
                assertEquals(WebRoute.Profiles, controller.route.value)
                assertNull(window.history.state)
                assertEquals(0, cancellations)
            } finally {
                controller.replace(WebRoute.Root)
                controller.close()
            }
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun awaitPinHistoryChange(action: () -> Unit) {
    suspendCancellableCoroutine { continuation ->
        var timer = 0
        lateinit var listener: (Event) -> Unit
        listener = {
            window.removeEventListener("popstate", listener)
            window.clearTimeout(timer)
            if (continuation.isActive) continuation.resume(Unit)
        }
        window.addEventListener("popstate", listener)
        timer = window.setTimeout({
            window.removeEventListener("popstate", listener)
            if (continuation.isActive) continuation.resumeWithException(AssertionError("PIN history did not complete its requested navigation"))
            null
        }, 5_000)
        continuation.invokeOnCancellation {
            window.removeEventListener("popstate", listener)
            window.clearTimeout(timer)
        }
        action()
    }
}
