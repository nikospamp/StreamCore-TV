package com.pampoukidis.streamcoretv.web.config

import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.Serializable

@Serializable
data class WebRuntimeConfig(
    val tmdbBaseUrl: String,
    val tmdbReadAccessToken: String,
    val tmdbAccountId: String,
) {
    fun validate(): WebRuntimeConfigValidationResult {
        val missingFields = buildList {
            if (tmdbBaseUrl.isBlank()) add("tmdbBaseUrl")
            if (tmdbReadAccessToken.isBlank()) add("tmdbReadAccessToken")
            if (tmdbAccountId.isBlank()) add("tmdbAccountId")
        }
        if (missingFields.isNotEmpty()) {
            return WebRuntimeConfigValidationResult.Invalid(
                "Required runtime configuration field(s) are blank: ${missingFields.joinToString()}.",
            )
        }

        val url = try {
            Url(tmdbBaseUrl)
        } catch (_: Exception) {
            return WebRuntimeConfigValidationResult.Invalid(
                "tmdbBaseUrl must be a valid absolute HTTPS URL.",
            )
        }
        if (url.protocol != URLProtocol.HTTPS || url.host.isBlank()) {
            return WebRuntimeConfigValidationResult.Invalid(
                "tmdbBaseUrl must be a valid absolute HTTPS URL.",
            )
        }

        return WebRuntimeConfigValidationResult.Valid(this)
    }

    fun toTmdbRuntimeConfig(): TmdbRuntimeConfig {
        return TmdbRuntimeConfig(
            baseUrl = tmdbBaseUrl,
            readAccessToken = tmdbReadAccessToken,
            accountId = tmdbAccountId,
        )
    }
}

sealed interface WebRuntimeConfigValidationResult {
    data class Valid(val config: WebRuntimeConfig) : WebRuntimeConfigValidationResult
    data class Invalid(val guidance: String) : WebRuntimeConfigValidationResult
}
