package com.pampoukidis.streamcoretv.core.model.general

import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val id: String,
    val name: String,
)