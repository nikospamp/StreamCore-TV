package com.pampoukidis.streamcore.sdk.providers.tmdb.auth

internal class TmdbAuthenticationFailureException(
    val backendCode: String,
    override val message: String,
) : RuntimeException(message)