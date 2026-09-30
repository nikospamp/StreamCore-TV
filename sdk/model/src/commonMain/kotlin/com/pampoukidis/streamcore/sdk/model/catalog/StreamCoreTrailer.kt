package com.pampoukidis.streamcore.sdk.model.catalog

import kotlinx.serialization.Serializable

/** An external trailer link, independent of the catalogue provider. */
@Serializable
data class StreamCoreTrailer(
    val id: String,
    val title: String,
    val url: String,
)
