package com.pampoukidis.streamcoretv.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.pampoukidis.streamcoretv.web.platform.configureWebImageLoader
import com.pampoukidis.streamcoretv.web.startup.WebStartup
import com.pampoukidis.streamcoretv.web.startup.WebStartupState
import com.pampoukidis.streamcoretv.web.ui.WebDiagnosticShell
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    configureWebImageLoader()
    var startupState: WebStartupState by mutableStateOf(WebStartupState.Loading)
    val applicationScope = MainScope()

    ComposeViewport(document.body!!) {
        WebDiagnosticShell(startupState)
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
    }
}
