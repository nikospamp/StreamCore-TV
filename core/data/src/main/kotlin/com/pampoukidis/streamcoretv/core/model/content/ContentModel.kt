package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import kotlin.text.equals
import kotlin.text.isBlank

data class ContentModel(
    val id: String,
    val title: String,
    val description: String,
    val rating: Int,
    val pgRatingName: String,
    val pgRatingLevel: Int,
    val poster: String,
    val backdrop: String?,
    val cast: List<Cast>,
    val releaseDate: Long,
    val genres: List<Genre>,
    val row: String? = null,
)

fun ContentModel.fallbackText(): String {
    return title.firstOrNull()?.uppercase() ?: "?"
}

fun ContentModel.imageUrl(style: RowStyle): String? {
    return when (style) {
        RowStyle.Carousel,
        RowStyle.Landscape -> backdrop ?: poster

        RowStyle.Poster,
        RowStyle.TopTen -> poster
    }
}

fun ContentModel.homeMetadataText(): String {
    val certification = pgRatingName.trim()
        .takeUnless { value ->
            value.isBlank()
                    || value.equals(UNRATED_CERTIFICATION_NAME, ignoreCase = true)
                    || value.equals(NOT_RATED_CERTIFICATION_NAME, ignoreCase = true)
        }

    if (certification == null) {
        return "$rating/10"
    }

    return "$rating/10 · $certification"
}

private const val UNRATED_CERTIFICATION_NAME = "NR"
private const val NOT_RATED_CERTIFICATION_NAME = "Not Rated"