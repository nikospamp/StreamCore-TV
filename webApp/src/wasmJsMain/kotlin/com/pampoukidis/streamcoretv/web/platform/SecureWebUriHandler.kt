package com.pampoukidis.streamcoretv.web.platform

import androidx.compose.ui.platform.UriHandler
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
object SecureWebUriHandler : UriHandler {
    override fun openUri(uri: String) {
        val url = try {
            Url(uri)
        } catch (_: Exception) {
            throw IllegalArgumentException("Only valid external HTTPS links can be opened.")
        }
        require(url.protocol == URLProtocol.HTTPS && url.host.isNotBlank()) {
            "Only valid external HTTPS links can be opened."
        }

        val opened = window.open(uri, "_blank", "noopener,noreferrer")
        opened?.opener = null
    }
}
