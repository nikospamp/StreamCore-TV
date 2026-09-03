package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileDto
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbProfilesPreferences(
    val schemaVersion: Int,
    val nextProfileNumber: Long,
    val profiles: List<ProfileDto>,
)
