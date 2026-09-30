package com.pampoukidis.streamcore.sdk.runtime.storage.search

import kotlinx.serialization.Serializable

@Serializable
internal data class RecentSearchPreferences(
    val queriesByProfile: Map<String, List<String>> = emptyMap(),
)
