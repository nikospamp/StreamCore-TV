package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.ui.platform.UriHandler
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import java.net.URI

internal fun openDetailsTrailer(
    trailer: TrailerModel,
    uriHandler: UriHandler,
    onError: (AppError) -> Unit,
) {
    try {
        val uri = URI.create(trailer.url)
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank())
        uriHandler.openUri(trailer.url)
    } catch (_: IllegalArgumentException) {
        onError(trailerLaunchError())
    } catch (_: SecurityException) {
        onError(trailerLaunchError())
    }
}

private fun trailerLaunchError(): AppError {
    return AppError.Unknown(
        source = ErrorSource(operation = "openTrailer", backendCode = "TRAILER_LAUNCH_UNAVAILABLE"),
    )
}
