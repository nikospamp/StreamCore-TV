package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbApiGenreDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbCastMemberDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieDetailsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbReleaseDateDto
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbReferenceData
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.roundToInt

internal fun TmdbMovieSummaryDto.toModel(referenceData: TmdbReferenceData): ContentModel {
    return ContentModel(
        id = id.toString(),
        title = title,
        description = overview,
        rating = voteAverage.toRating(),
        pgRatingName = adult.toSummaryCertificationName(),
        pgRatingLevel = adult.toSummaryCertificationLevel(),
        poster = referenceData.posterUrl(posterPath).orEmpty(),
        backdrop = referenceData.backdropUrl(backdropPath),
        cast = emptyList(),
        releaseDate = releaseDate.toEpochMillis(),
        genres = genreIds.mapNotNull { genreId ->
            referenceData.genre(genreId)?.toModel()
        },
    )
}

internal fun TmdbMovieDetailsDto.toModel(
    referenceData: TmdbReferenceData,
    countryCode: String = DEFAULT_CERTIFICATION_COUNTRY,
): ContentModel {
    val certificationName = certificationName(countryCode = countryCode)

    return ContentModel(
        id = id.toString(),
        title = title,
        description = overview,
        rating = voteAverage.toRating(),
        pgRatingName = certificationName ?: adult.toSummaryCertificationName(),
        pgRatingLevel = certificationName?.toCertificationLevel()
            ?: adult.toSummaryCertificationLevel(),
        poster = referenceData.posterUrl(posterPath).orEmpty(),
        backdrop = referenceData.backdropUrl(backdropPath),
        cast = credits?.cast.orEmpty()
            .sortedBy { castMember -> castMember.order }
            .take(MAX_CAST_MEMBERS)
            .map { castMember -> castMember.toModel(referenceData = referenceData) },
        releaseDate = releaseDate.toEpochMillis(),
        genres = genres.map { genre -> genre.toModel() },
    )
}

internal fun List<TmdbMovieSummaryDto>.toModels(
    referenceData: TmdbReferenceData,
): List<ContentModel> {
    return map { movie -> movie.toModel(referenceData = referenceData) }
}

private fun TmdbCastMemberDto.toModel(referenceData: TmdbReferenceData): Cast {
    return Cast(
        id = id.toString(),
        name = name,
        characterName = character,
        image = referenceData.profileUrl(profilePath),
    )
}

private fun TmdbApiGenreDto.toModel(): Genre {
    return Genre(
        id = id.toString(),
        name = name,
    )
}

private fun Double.toRating(): Int {
    return roundToInt().coerceIn(MIN_RATING, MAX_RATING)
}

private fun Boolean.toSummaryCertificationName(): String {
    if (this) {
        return ADULT_CERTIFICATION_NAME
    }

    return UNRATED_CERTIFICATION_NAME
}

private fun Boolean.toSummaryCertificationLevel(): Int {
    if (this) {
        return ADULT_CERTIFICATION_LEVEL
    }

    return UNRATED_CERTIFICATION_LEVEL
}

private fun TmdbMovieDetailsDto.certificationName(countryCode: String): String? {
    return releaseDates?.results
        ?.firstOrNull { result -> result.countryCode.equals(countryCode, ignoreCase = true) }
        ?.releaseDates
        ?.filter { releaseDate -> releaseDate.certification.isNotBlank() }
        ?.sortedBy { releaseDate -> releaseDate.certificationPriority() }
        ?.firstOrNull()
        ?.certification
}

private fun TmdbReleaseDateDto.certificationPriority(): Int {
    return when (type) {
        THEATRICAL_RELEASE_TYPE -> 0
        THEATRICAL_LIMITED_RELEASE_TYPE -> 1
        DIGITAL_RELEASE_TYPE -> 2
        else -> 3
    }
}

private fun String.toCertificationLevel(): Int {
    return when (trim().uppercase()) {
        "G",
        "U",
        "ALL" -> 0

        "PG",
        "TV-PG" -> 7

        "PG-13",
        "TV-14" -> 13

        "R",
        "TV-MA" -> 17

        "NC-17",
        "18",
        "18+" -> 18

        "16",
        "16+" -> 16

        "15",
        "15+" -> 15

        "12",
        "12+" -> 12

        "7",
        "7+" -> 7

        else -> UNRATED_CERTIFICATION_LEVEL
    }
}

private fun String?.toEpochMillis(): Long {
    if (isNullOrBlank()) {
        return 0L
    }

    val parts = split("-")
    if (parts.size != DATE_PART_COUNT) {
        return 0L
    }

    val year = parts[0].toIntOrNull() ?: return 0L
    val month = parts[1].toIntOrNull() ?: return 0L
    val day = parts[2].toIntOrNull() ?: return 0L

    return GregorianCalendar(TimeZone.getTimeZone(UTC_TIME_ZONE)).apply {
        clear()
        set(year, month - 1, day, 0, 0, 0)
    }.timeInMillis
}

private const val DEFAULT_CERTIFICATION_COUNTRY = "US"
private const val MIN_RATING = 0
private const val MAX_RATING = 10
private const val MAX_CAST_MEMBERS = 10
private const val UNRATED_CERTIFICATION_NAME = "NR"
private const val ADULT_CERTIFICATION_NAME = "18+"
private const val UNRATED_CERTIFICATION_LEVEL = 0
private const val ADULT_CERTIFICATION_LEVEL = 18
private const val THEATRICAL_RELEASE_TYPE = 3
private const val THEATRICAL_LIMITED_RELEASE_TYPE = 2
private const val DIGITAL_RELEASE_TYPE = 4
private const val DATE_PART_COUNT = 3
private const val UTC_TIME_ZONE = "UTC"
