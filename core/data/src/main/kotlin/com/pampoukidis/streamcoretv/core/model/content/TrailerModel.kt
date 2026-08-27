package com.pampoukidis.streamcoretv.core.model.content

import kotlinx.serialization.Serializable

/** An external trailer link, independent of the catalogue provider. */
@Serializable
data class TrailerModel(
    val id: String,
    val title: String,
    val url: String,
)
