package com.pampoukidis.streamcore.sdk.providers.tmdb.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbDeleteSessionResponseDto(
    val success: Boolean = false,
)