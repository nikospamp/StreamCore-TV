package com.pampoukidis.streamcoretv.data.tmdb.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbConfigurationDto(
    val images: TmdbImagesConfigurationDto,
)