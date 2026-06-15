package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbCastMemberDto(
    val id: Int,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path")
    val profilePath: String? = null,
    val order: Int = Int.MAX_VALUE,
)