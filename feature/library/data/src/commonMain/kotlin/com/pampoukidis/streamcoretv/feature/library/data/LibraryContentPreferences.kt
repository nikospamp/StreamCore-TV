package com.pampoukidis.streamcoretv.feature.library.data

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.general.Genre
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
    val genres: List<Genre>,
)

internal fun ContentModel.toLibraryPreferences(): LibraryContentPreferences {
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

internal fun LibraryContentPreferences.toModel(): ContentModel {
    return ContentModel(
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
