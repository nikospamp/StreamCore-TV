package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbReleaseDatesCountryDto(
    @SerialName("iso_3166_1")
    val countryCode: String,
    @SerialName("release_dates")
    val releaseDates: List<TmdbReleaseDateDto> = emptyList(),
)