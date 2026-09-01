package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.ui.platform.UriHandler
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import io.ktor.http.URLProtocol
import io.ktor.http.URLDecodeException
import io.ktor.http.decodeURLPart
import io.ktor.http.hostIsIp
import io.ktor.http.parseUrl

internal fun openDetailsTrailer(
    trailer: TrailerModel,
    uriHandler: UriHandler,
    onError: (AppError) -> Unit,
) {
    try {
        require(isValidHttpsUrl(trailer.url))
        uriHandler.openUri(trailer.url)
    } catch (_: IllegalArgumentException) {
        onError(trailerLaunchError())
    } catch (_: SecurityException) {
        onError(trailerLaunchError())
    }
}

private fun isValidHttpsUrl(url: String): Boolean {
    if (!url.startsWith("https://", ignoreCase = true)) {
        return false
    }
    val authorityStart = HttpsPrefixLength
    val authorityEnd = url.indexOfAny(
        chars = charArrayOf('/', '?', '#'),
        startIndex = authorityStart,
    ).takeIf { index -> index >= 0 } ?: url.length
    if (url.substring(authorityStart, authorityEnd).isBlank()) {
        return false
    }
    try {
        url.decodeURLPart()
    } catch (_: URLDecodeException) {
        return false
    }
    val parsedUrl = parseUrl(url) ?: return false
    return parsedUrl.protocol == URLProtocol.HTTPS && isValidHost(parsedUrl.host)
}

private fun isValidHost(host: String): Boolean {
    if (hostIsIp(host)) {
        return host.startsWith('[') || isValidIpv4Address(host)
    }
    val normalizedHost = host.removeSuffix(".")
    if (normalizedHost.isBlank() || normalizedHost.length > MaximumHostLength) {
        return false
    }
    return normalizedHost.split('.').all { label ->
        label.isNotEmpty() &&
            label.length <= MaximumHostLabelLength &&
            label.first().isLetterOrDigit() &&
            label.last().isLetterOrDigit() &&
            label.all { character -> character.isLetterOrDigit() || character == '-' }
    }
}

private fun isValidIpv4Address(host: String): Boolean {
    val segments = host.split('.')
    return segments.size == Ipv4SegmentCount && segments.all { segment ->
        segment.isNotEmpty() &&
            segment.all(Char::isDigit) &&
            (segment.toIntOrNull() ?: return@all false) in Ipv4SegmentRange
    }
}

private fun trailerLaunchError(): AppError {
    return AppError.Unknown(
        source = ErrorSource(operation = "openTrailer", backendCode = "TRAILER_LAUNCH_UNAVAILABLE"),
    )
}

private const val MaximumHostLength = 253
private const val MaximumHostLabelLength = 63
private const val HttpsPrefixLength = 8
private const val Ipv4SegmentCount = 4
private val Ipv4SegmentRange = 0..255
