package com.pampoukidis.streamcore.sdk.providers.tmdb.profile

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbProfilesPreferences(
    val schemaVersion: Int,
    val nextProfileNumber: Long,
    val profiles: List<ProfileDto>,
)
