package com.pampoukidis.streamcore.sdk.runtime.storage.library

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreGenre
import kotlinx.serialization.Serializable

@Serializable
internal data class LibraryContentPreferences(
    val id: String,
    val title: String,
    val rating: Int,
    val pgRatingName: String,
    val pgRatingLevel: Int,
    val poster: String,
    val backdrop: String?,
    val releaseDate: Long,
    val genres: List<StreamCoreGenre>,
)

internal fun StreamCoreContent.toLibraryPreferences(): LibraryContentPreferences {
    return LibraryContentPreferences(
        id = id,
        title = title,
        rating = rating,
        pgRatingName = pgRatingName,
        pgRatingLevel = pgRatingLevel,
        poster = poster,
        backdrop = backdrop,
        releaseDate = releaseDate,
        genres = genres,
    )
}

internal fun LibraryContentPreferences.toModel(): StreamCoreContent {
    return StreamCoreContent(
        id = id,
        title = title,
        description = "",
        rating = rating,
        pgRatingName = pgRatingName,
        pgRatingLevel = pgRatingLevel,
        poster = poster,
        backdrop = backdrop,
        cast = emptyList(),
        releaseDate = releaseDate,
        genres = genres,
        row = null,
        playbackProgress = null,
    )
}
