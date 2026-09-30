package com.pampoukidis.streamcore.sdk.runtime.storage.library

import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibraryEntry
import kotlinx.serialization.Serializable

@Serializable
internal data class LibraryEntryPreferences(
    val content: LibraryContentPreferences,
    val likedAtMillis: Long? = null,
    val addedToMyListAtMillis: Long? = null,
)

internal fun LibraryEntryPreferences.toModel(): StreamCoreLibraryEntry {
    return StreamCoreLibraryEntry(
        content = content.toModel(),
        likedAtMillis = likedAtMillis,
        addedToMyListAtMillis = addedToMyListAtMillis,
    )
}
