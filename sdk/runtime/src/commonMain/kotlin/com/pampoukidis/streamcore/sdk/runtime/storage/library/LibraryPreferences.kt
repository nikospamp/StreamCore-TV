package com.pampoukidis.streamcore.sdk.runtime.storage.library

import kotlinx.serialization.Serializable

@Serializable
internal data class LibraryPreferences(
    val version: Int = CurrentLibraryPreferencesVersion,
    val entriesByProfile: Map<String, List<LibraryEntryPreferences>> = emptyMap(),
)

private const val CurrentLibraryPreferencesVersion = 1
