package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbCreditsDto(
    val cast: List<TmdbCastMemberDto> = emptyList(),
)