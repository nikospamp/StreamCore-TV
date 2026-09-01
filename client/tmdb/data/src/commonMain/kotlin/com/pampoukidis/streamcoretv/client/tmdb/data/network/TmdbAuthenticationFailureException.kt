package com.pampoukidis.streamcoretv.client.tmdb.data.network

internal class TmdbAuthenticationFailureException(
    val backendCode: String,
    override val message: String,
) : RuntimeException(message)