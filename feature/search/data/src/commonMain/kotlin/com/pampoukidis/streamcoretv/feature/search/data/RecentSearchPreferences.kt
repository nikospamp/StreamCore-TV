package com.pampoukidis.streamcoretv.feature.search.data

import kotlinx.serialization.Serializable

@Serializable
internal data class RecentSearchPreferences(
    val queriesByProfile: Map<String, List<String>> = emptyMap(),
)
