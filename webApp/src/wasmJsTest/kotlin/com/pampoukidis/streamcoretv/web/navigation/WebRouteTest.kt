package com.pampoukidis.streamcoretv.web.navigation

import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebRouteTest {
    @Test
    fun parsesDirectDetailsAndPlayerUrlsWithIdsOnly() {
        assertEquals(WebRoute.Details("603"), WebRoute.parse("/details/603"))
        assertEquals(WebRoute.Player("movie_603"), WebRoute.parse("/player/movie_603"))
    }

    @Test
    fun parsesCanonicalBrowseAndDiagnosticRoutes() {
        assertEquals(WebRoute.Home, WebRoute.parse("/home"))
        assertEquals(WebRoute.Search, WebRoute.parse("/search"))
        assertEquals(WebRoute.Library, WebRoute.parse("/library"))
        assertEquals(
            WebRoute.DiagnosticDetails("603"),
            WebRoute.parse("/diagnostic/details/603"),
        )
        assertEquals(
            WebRoute.DiagnosticPlayer("movie_603"),
            WebRoute.parse("/diagnostic/player/movie_603"),
        )
    }

    @Test
    fun rejectsPayloadLikeAndUnknownRoutes() {
        assertEquals(WebRoute.Root, WebRoute.parse("/details/%7Bcontent%7D"))
        assertEquals(WebRoute.Root, WebRoute.parse("/unknown/603"))
        assertFailsWith<IllegalArgumentException> { WebRoute.Details("603/credits") }
        assertFailsWith<IllegalArgumentException> { WebRoute.EditProfile("profile?manage=true") }
    }

    @Test
    fun routePathsRemainBrowserSafe() {
        assertEquals("/details/movie-603", WebRoute.Details("movie-603").path)
        assertEquals("/player/movie_603", WebRoute.Player("movie_603").path)
        assertEquals("/home", WebRoute.Home.path)
        assertEquals("/search", WebRoute.Search.path)
        assertEquals("/library", WebRoute.Library.path)
        assertEquals(WebRoute.Login, WebRoute.parse("/login?source=history"))
        assertEquals(WebRoute.EditProfile("profile-1"), WebRoute.parse("/profiles/profile-1/edit"))
    }

    @Test
    fun historyFocusStateRoundTripsStrictly() {
        val key = WebBrowseFocusKey(
            destination = WebBrowseDestination.Home,
            sectionKey = "featured",
            itemKey = "movie-603",
        )

        assertEquals(key, WebHistoryStateCodec.decode(WebHistoryStateCodec.encode(key)))
        assertNull(WebHistoryStateCodec.decode(null))
        assertNull(WebHistoryStateCodec.decode("not-json"))
        assertNull(
            WebHistoryStateCodec.decode(
                """{"version":2,"destination":"home","sectionKey":"featured","itemKey":"603"}""",
            ),
        )
    }

    @Test
    fun navigationControllerCapturesAndConsumesOnlyMatchingFocus() {
        val controller = WebNavigationController()
        controller.replace(WebRoute.Home)
        val homeKey = WebBrowseFocusKey(
            destination = WebBrowseDestination.Home,
            sectionKey = "featured",
            itemKey = "603",
        )
        val searchKey = homeKey.copy(destination = WebBrowseDestination.Search)

        assertFalse(controller.captureReturnFocus(searchKey))
        assertTrue(controller.captureReturnFocus(homeKey))
        assertEquals(homeKey, controller.entry.value.returnFocusKey)
        assertFalse(controller.consumeReturnFocus(searchKey))
        assertTrue(controller.consumeReturnFocus(homeKey))
        assertNull(controller.entry.value.returnFocusKey)
        controller.close()
    }

    @Test
    fun browserBackRestoresAndConsumesHomeAndDetailsFocusEntries(): TestResult {
        return runTest {
            val controller = WebNavigationController()
            val homeKey = WebBrowseFocusKey(
                destination = WebBrowseDestination.Home,
                sectionKey = "featured",
                itemKey = "603",
            )
            controller.replace(WebRoute.Home)
            assertTrue(controller.captureReturnFocus(homeKey))
            controller.navigate(WebRoute.Details("603"))

            awaitPopState { window.history.back() }

            assertEquals(WebRoute.Home, controller.entry.value.route)
            assertEquals(homeKey, controller.entry.value.returnFocusKey)
            assertTrue(controller.consumeReturnFocus(homeKey))
            assertNull(controller.entry.value.returnFocusKey)

            val playKey = WebBrowseFocusKey(
                destination = WebBrowseDestination.Details,
                sectionKey = "details:actions",
                itemKey = "play",
            )
            controller.replace(WebRoute.Details("603"))
            assertTrue(controller.captureReturnFocus(playKey))
            controller.navigate(WebRoute.Player("603"))

            awaitPopState { window.history.back() }

            assertEquals(WebRoute.Details("603"), controller.entry.value.route)
            assertEquals(playKey, controller.entry.value.returnFocusKey)
            assertTrue(controller.consumeReturnFocus(playKey))
            assertNull(controller.entry.value.returnFocusKey)
            controller.replace(WebRoute.Root)
            controller.close()
        }
    }
}

private suspend fun awaitPopState(action: () -> Unit) {
    suspendCancellableCoroutine { continuation ->
        lateinit var listener: (Event) -> Unit
        listener = {
            window.removeEventListener("popstate", listener)
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
        window.addEventListener("popstate", listener)
        continuation.invokeOnCancellation {
            window.removeEventListener("popstate", listener)
        }
        action()
    }
}
