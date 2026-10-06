package com.pampoukidis.streamcore.sdk.providers.tmdb.catalog

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCastMember
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreGenre
import com.pampoukidis.streamcore.sdk.providers.tmdb.details.TmdbCastMemberDto
import com.pampoukidis.streamcore.sdk.providers.tmdb.details.TmdbMovieDetailsDto
import com.pampoukidis.streamcore.sdk.providers.tmdb.details.TmdbReleaseDateDto
import com.pampoukidis.streamcore.sdk.providers.tmdb.details.toTrailers
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

internal fun TmdbMovieSummaryDto.toContentModel(
    referenceData: TmdbReferenceData,
    row: String? = null,
): StreamCoreContent {
    return StreamCoreContent(
        id = id.toString(),
        title = title,
        description = overview,
        rating = voteAverage.toRating(),
        pgRatingName = getSummaryCertificationName(adult),
        pgRatingLevel = getSummaryCertificationLevel(adult),
        poster = referenceData.posterUrl(posterPath).orEmpty(),
        backdrop = referenceData.backdropUrl(backdropPath),
        cast = emptyList(),
        releaseDate = releaseDate.toEpochMillis(),
        genres = genreIds.mapNotNull { genreId ->
            referenceData.genre(genreId)?.toGenre()
        },
        row = row,
    )
}

internal fun TmdbMovieDetailsDto.toModel(
    referenceData: TmdbReferenceData,
    countryCode: String = DEFAULT_CERTIFICATION_COUNTRY,
): StreamCoreContent {
    val certificationName = certificationName(countryCode = countryCode)

    return StreamCoreContent(
        id = id.toString(),
        title = title,
        description = overview,
        rating = voteAverage.toRating(),
        pgRatingName = certificationName ?: getSummaryCertificationName(adult),
        pgRatingLevel = getCertificationLevel(certificationName),
        poster = referenceData.posterUrl(posterPath).orEmpty(),
        backdrop = referenceData.backdropUrl(backdropPath),
        cast = credits?.cast.orEmpty()
            .sortedBy { castMember -> castMember.order }
            .take(MAX_CAST_MEMBERS)
            .map { castMember -> castMember.toCast(referenceData = referenceData) },
        releaseDate = releaseDate.toEpochMillis(),
        genres = genres.map { genre -> genre.toGenre() },
        trailers = videos?.toTrailers().orEmpty(),
    )
}

internal fun List<TmdbMovieSummaryDto>.toModels(
    referenceData: TmdbReferenceData,
    row: String? = null,
): List<StreamCoreContent> {
    return map { movie ->
        movie.toContentModel(
            referenceData = referenceData,
            row = row,
        )
    }
}

internal fun TmdbApiGenreDto.toGenre(): StreamCoreGenre {
    return StreamCoreGenre(
        id = id.toString(),
        name = name,
    )
}

internal fun TmdbCastMemberDto.toCast(referenceData: TmdbReferenceData): StreamCoreCastMember {
    return StreamCoreCastMember(
        id = id.toString(),
        name = name,
        characterName = character,
        image = referenceData.profileUrl(profilePath),
    )
}

private fun Double.toRating(): Int {
    return roundToInt().coerceIn(MIN_RATING, MAX_RATING)
}

private fun getSummaryCertificationName(value: Boolean): String {
    if (value) {
        return ADULT_CERTIFICATION_NAME
    }

    return UNRATED_CERTIFICATION_NAME
}

private fun getSummaryCertificationLevel(value: Boolean): Int {
    if (value) {
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

internal fun getCertificationLevel(value: String?): Int {
    return when (value?.trim()?.uppercase()) {
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

@OptIn(ExperimentalTime::class)
private fun String?.toEpochMillis(): Long {
    if (isNullOrBlank()) {
        return 0L
    }

    return runCatching {
        LocalDate.parse(this).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }.getOrDefault(0L)
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
