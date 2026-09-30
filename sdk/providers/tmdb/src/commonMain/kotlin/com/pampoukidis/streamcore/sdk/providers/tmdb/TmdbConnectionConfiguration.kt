package com.pampoukidis.streamcore.sdk.providers.tmdb

/** Connection settings only; optional account restrictions belong to the common SDK configuration. */
data class TmdbConnectionConfiguration(
    val baseUrl: String,
    val readAccessToken: String,
) {
    override fun toString(): String {
        return "TmdbConnectionConfiguration(baseUrl=$baseUrl, readAccessToken=[REDACTED])"
    }
}
