package com.pampoukidis.streamcoretv.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.pampoukidis.streamcoretv.web.platform.configureWebImageLoader
import com.pampoukidis.streamcoretv.web.network.WebTmdbFetchProbe
import com.pampoukidis.streamcoretv.web.network.WebTmdbFetchProbeResult
import com.pampoukidis.streamcoretv.web.startup.WebStartup
import com.pampoukidis.streamcoretv.web.startup.WebStartupState
import com.pampoukidis.streamcoretv.web.ui.WebProductShell
import com.pampoukidis.streamcoretv.web.navigation.WebRoute
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    configureWebImageLoader()
    var startupState: WebStartupState by mutableStateOf(WebStartupState.Loading)
    val applicationScope = MainScope()

    ComposeViewport(document.body!!) {
        WebProductShell(startupState)
    }

    applicationScope.launch {
        startupState = WebStartup().start()
        document.body?.setAttribute(
            "data-runtime-state",
            when (startupState) {
                WebStartupState.Loading -> "loading"
                is WebStartupState.BlockingError -> "blocking-error"
                is WebStartupState.Ready -> "ready"
            },
        )
        val readyState = startupState as? WebStartupState.Ready
        if (readyState != null) {
            document.body?.setAttribute(
                "data-storage-mode",
                if (readyState.useSessionStorage) "session" else "persistent",
            )
            if (readyState.navigationController.route.value is WebRoute.Diagnostic) {
                document.body?.setAttribute(
                    "data-network-probe",
                    when (val result = WebTmdbFetchProbe().run(readyState.graph)) {
                        WebTmdbFetchProbeResult.Success -> "success"
                        is WebTmdbFetchProbeResult.Failure -> result.code
                    },
                )
            } else {
                document.body?.removeAttribute("data-network-probe")
            }
        } else {
            document.body?.removeAttribute("data-storage-mode")
            document.body?.removeAttribute("data-network-probe")
        }
    }
}
