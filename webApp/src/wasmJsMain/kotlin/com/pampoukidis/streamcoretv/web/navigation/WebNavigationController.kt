package com.pampoukidis.streamcoretv.web.navigation

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
class WebNavigationController : AutoCloseable {
    private val _route = MutableStateFlow(WebRoute.parse(window.location.pathname))
    val route: StateFlow<WebRoute> = _route.asStateFlow()

    private val popStateListener: (Event) -> Unit = { _: Event ->
        _route.value = WebRoute.parse(window.location.pathname)
    }

    init {
        window.addEventListener("popstate", popStateListener)
    }

    fun navigate(route: WebRoute) {
        if (route == _route.value) {
            return
        }
        window.history.pushState(null, "", route.path)
        _route.value = route
    }

    fun replace(route: WebRoute) {
        window.history.replaceState(null, "", route.path)
        _route.value = route
    }

    override fun close() {
        window.removeEventListener("popstate", popStateListener)
    }
}
