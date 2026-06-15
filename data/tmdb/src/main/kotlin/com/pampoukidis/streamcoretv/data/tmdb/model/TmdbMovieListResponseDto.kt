package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbMovieListResponseDto(
    val page: Int,
    val results: List<TmdbMovieSummaryDto>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int,
)