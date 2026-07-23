package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbImagesConfigurationDto(
    @SerialName("secure_base_url")
    val secureBaseUrl: String,
    @SerialName("poster_sizes")
    val posterSizes: List<String>,
    @SerialName("backdrop_sizes")
    val backdropSizes: List<String>,
    @SerialName("profile_sizes")
    val profileSizes: List<String>,
)