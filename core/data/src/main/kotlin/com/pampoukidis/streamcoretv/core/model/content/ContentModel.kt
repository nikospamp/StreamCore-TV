package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.TimeZone

@Serializable
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
    val playbackProgress: PlaybackProgressModel? = null,
)

fun ContentModel.fallbackText(): String {
    return title.firstOrNull()?.uppercase() ?: "?"
}

fun ContentModel.imageUrl(type: RowType): String? {
    return when (type) {
        RowType.Featured,
        RowType.ContinueWatching,
        RowType.Landscape -> backdrop ?: poster

        RowType.Poster, RowType.TopTen -> poster
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

fun ContentModel.heroMetadata(): String {
    val values = buildList {
        val year = releaseYear(releaseDate)
        if (year > 0) {
            add(year.toString())
        }
        genres.firstOrNull()?.name?.let(::add)
        add(homeMetadataText())
    }
    return values.joinToString(separator = "  ·  ")
}

private fun releaseYear(epochMillis: Long): Int {
    if (epochMillis <= 0L) {
        return 0
    }

    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        timeInMillis = epochMillis
        get(Calendar.YEAR)
    }
}

private const val UNRATED_CERTIFICATION_NAME = "NR"
private const val NOT_RATED_CERTIFICATION_NAME = "Not Rated"
