package com.pampoukidis.streamcore.sdk.providers.tmdb.details

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbCreditsDto(
    val cast: List<TmdbCastMemberDto> = emptyList(),
)