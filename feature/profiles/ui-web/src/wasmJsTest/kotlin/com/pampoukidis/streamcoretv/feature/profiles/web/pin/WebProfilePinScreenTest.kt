package com.pampoukidis.streamcoretv.feature.profiles.web.pin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.*
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebProfilePinScreenTest {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun browserInputIsMaskedFiltersDigitsAndEscapeCancelsDuringChecking(): TestResult {
        var state by mutableStateOf(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4))
        var visible by mutableStateOf(true)
        var cancellations = 0
        return runTest {
            val host = (document.createElement("div") as HTMLDivElement).apply {
                style.cssText = "position:fixed;inset:0;width:1280px;height:720px;"
            }
            requireNotNull(document.body).appendChild(host)
            ComposeViewport(host) {
                StreamCoreTheme {
                    if (visible) {
                        WebProfilePinScreen(state, { state = state.copy(draft = it) }, { cancellations++; visible = false }, {})
                    }
                }
            }
            try {
                awaitBrowserCondition(failureMessage = "PIN input label did not resolve from processed Compose resources") {
                    !host.querySelector(PinInputSelector)?.getAttribute("aria-label").isNullOrBlank()
                }
                val input = host.querySelector(PinInputSelector) as? HTMLInputElement
                assertNotNull(input)
                assertEquals("password", input.type)
                assertEquals("numeric", input.getAttribute("inputmode"))
                assertEquals("Profile PIN", input.getAttribute("aria-label"))
                input.value = "1a2"
                input.dispatchEvent(Event("input"))
                assertEquals("12", state.draft)
                assertEquals("12", input.value)
                state = state.copy(isSubmitting = true)
                awaitBrowserCondition { input.disabled }
                assertTrue(input.disabled)
                document.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Escape", bubbles = true, cancelable = true)))
                awaitBrowserCondition { host.querySelector(PinInputSelector) == null }
                assertEquals(1, cancellations)
                assertEquals("", input.value)
                assertNull(host.querySelector(PinInputSelector))
                document.dispatchEvent(KeyboardEvent("keydown", KeyboardEventInit(key = "Escape", bubbles = true, cancelable = true)))
                assertEquals(1, cancellations, "The disposed input must release its document Escape listener")
            } finally {
                visible = false
                try {
                    awaitBrowserCondition(timeoutMillis = 1_000.0) { host.querySelector(PinInputSelector) == null }
                } finally {
                    host.parentNode?.removeChild(host)
                }
            }
        }
    }
}

private const val PinInputSelector = "[data-testid='" + ProfilePinTestTags.Input + "']"

/** Real viewport frames cannot be driven by the virtual Compose test clock. */
@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun awaitBrowserCondition(
    failureMessage: String = "PIN viewport did not reach the expected DOM state",
    timeoutMillis: Double = 5_000.0,
    condition: () -> Boolean,
) {
    val deadline = window.performance.now() + timeoutMillis
    while (!condition()) {
        check(window.performance.now() < deadline) { failureMessage }
        suspendCancellableCoroutine { continuation ->
            val timer = window.setTimeout({
                if (continuation.isActive) continuation.resume(Unit)
                null
            }, 16)
            continuation.invokeOnCancellation { window.clearTimeout(timer) }
        }
    }
}
