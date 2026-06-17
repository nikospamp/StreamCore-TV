package com.pampoukidis.streamcoretv.data.tmdb.network

internal class TmdbAuthenticationFailureException(
    val backendCode: String,
    override val message: String,
) : RuntimeException(message)