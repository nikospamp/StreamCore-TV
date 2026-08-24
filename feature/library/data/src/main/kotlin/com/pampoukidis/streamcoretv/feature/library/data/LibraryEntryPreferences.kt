package com.pampoukidis.streamcoretv.feature.library.data

import com.pampoukidis.streamcoretv.core.model.library.LibraryEntryModel
import kotlinx.serialization.Serializable

@Serializable
internal data class LibraryEntryPreferences(
    val content: LibraryContentPreferences,
    val likedAtMillis: Long? = null,
    val addedToMyListAtMillis: Long? = null,
)

internal fun LibraryEntryPreferences.toModel(): LibraryEntryModel {
    return LibraryEntryModel(
        content = content.toModel(),
        likedAtMillis = likedAtMillis,
        addedToMyListAtMillis = addedToMyListAtMillis,
    )
}
