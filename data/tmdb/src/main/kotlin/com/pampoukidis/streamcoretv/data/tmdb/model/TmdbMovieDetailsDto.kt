package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbMovieDetailsDto(
    val id: Int,
    val title: String,
    val overview: String,
    val adult: Boolean = false,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    val genres: List<TmdbApiGenreDto> = emptyList(),
    val credits: TmdbCreditsDto? = null,
    @SerialName("release_dates")
    val releaseDates: TmdbReleaseDatesResponseDto? = null,
)
