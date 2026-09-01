package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.ui.platform.UriHandler
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource

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
    return HttpsUrlRegex.matches(url)
}

private fun trailerLaunchError(): AppError {
    return AppError.Unknown(
        source = ErrorSource(operation = "openTrailer", backendCode = "TRAILER_LAUNCH_UNAVAILABLE"),
    )
}

private val HttpsUrlRegex = Regex(
    pattern = "^https://[^\\s/?#]+(?:[/?#][^\\s]*)?$",
    option = RegexOption.IGNORE_CASE,
)
