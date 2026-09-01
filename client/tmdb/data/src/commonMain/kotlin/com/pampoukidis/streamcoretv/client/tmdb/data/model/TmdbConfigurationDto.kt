package com.pampoukidis.streamcoretv.client.tmdb.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbConfigurationDto(
    val images: TmdbImagesConfigurationDto,
)