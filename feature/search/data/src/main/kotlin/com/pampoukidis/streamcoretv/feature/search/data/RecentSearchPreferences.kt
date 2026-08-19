package com.pampoukidis.streamcoretv.feature.search.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class RecentSearchPreferences(
    val queriesByProfile: Map<String, List<String>> = emptyMap(),
)
