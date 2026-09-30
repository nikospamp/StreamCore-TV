package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun StreamCoreContent.fallbackText(): String {
    return title.firstOrNull()?.uppercase() ?: "?"
}

fun StreamCoreContent.imageUrl(type: RowType): String? {
    return when (type) {
        RowType.Featured,
        RowType.ContinueWatching,
        RowType.Landscape -> backdrop ?: poster

        RowType.Poster, RowType.TopTen -> poster
    }
}

fun StreamCoreContent.homeMetadataText(): String {
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

fun StreamCoreContent.heroMetadata(): String {
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

    return Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.UTC)
        .year
}

private const val UNRATED_CERTIFICATION_NAME = "NR"
private const val NOT_RATED_CERTIFICATION_NAME = "Not Rated"
