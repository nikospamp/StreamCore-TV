package com.pampoukidis.streamcoretv.web.navigation

import kotlinx.browser.window
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

@OptIn(ExperimentalWasmJsInterop::class)
class WebNavigationController : AutoCloseable {
    private val _entry = MutableStateFlow(readBrowserEntry())
    internal val entry: StateFlow<WebNavigationEntry> = _entry.asStateFlow()

    private val _route = MutableStateFlow(_entry.value.route)
    val route: StateFlow<WebRoute> = _route.asStateFlow()
    private var pinBackHandler: (() -> Unit)? = null
    private var ownsPinEntry = false

    private val popStateListener: (Event) -> Unit = { _: Event ->
        if (ownsPinEntry) {
            ownsPinEntry = false
            val cancel = pinBackHandler
            pinBackHandler = null
            cancel?.invoke()
        }
        updateEntry(readBrowserEntry())
    }

    init {
        window.addEventListener("popstate", popStateListener)
    }

    fun navigate(route: WebRoute) {
        if (route == _route.value) {
            return
        }
        if (ownsPinEntry) {
            ownsPinEntry = false
            pinBackHandler = null
            writeReplacement(WebNavigationEntry(route = route))
            return
        }
        window.history.pushState(null, "", route.path)
        updateEntry(WebNavigationEntry(route = route))
    }

    fun replace(route: WebRoute) {
        ownsPinEntry = false
        pinBackHandler = null
        writeReplacement(WebNavigationEntry(route = route))
    }

    /** A transient same-URL entry lets browser Back cancel PIN entry without storing the PIN. */
    fun setProfilePinBackHandler(handler: (() -> Unit)?, activated: Boolean = false) {
        pinBackHandler = handler
        if (handler != null && !ownsPinEntry) {
            check(_route.value is WebRoute.Profiles)
            window.history.pushState("streamcore-profile-pin".toJsString(), "", WebRoute.Profiles.path)
            ownsPinEntry = true
        } else if (handler == null && ownsPinEntry && !activated) {
            ownsPinEntry = false
            window.history.back()
        }
    }

    fun captureReturnFocus(key: WebBrowseFocusKey): Boolean {
        val current = _entry.value
        if (current.route.browseDestinationOrNull() != key.destination) {
            return false
        }
        writeReplacement(current.copy(returnFocusKey = key))
        return true
    }

    fun consumeReturnFocus(key: WebBrowseFocusKey): Boolean {
        val current = _entry.value
        if (current.returnFocusKey != key) {
            return false
        }
        writeReplacement(current.copy(returnFocusKey = null))
        return true
    }

    override fun close() {
        pinBackHandler = null
        ownsPinEntry = false
        window.removeEventListener("popstate", popStateListener)
    }

    private fun writeReplacement(entry: WebNavigationEntry) {
        val encodedState = WebHistoryStateCodec.encode(entry.returnFocusKey)?.toJsString()
        window.history.replaceState(encodedState, "", entry.route.path)
        updateEntry(entry)
    }

    private fun updateEntry(entry: WebNavigationEntry) {
        _entry.value = entry
        _route.value = entry.route
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun readBrowserEntry(): WebNavigationEntry {
    val route = WebRoute.parse(window.location.pathname)
    val decodedFocus = WebHistoryStateCodec.decode(window.history.state?.toString())
        ?.takeIf { key -> route.browseDestinationOrNull() == key.destination }
    return WebNavigationEntry(
        route = route,
        returnFocusKey = decodedFocus,
    )
}
