package com.pampoukidis.streamcoretv.core.model.general

import kotlinx.serialization.Serializable

@Serializable
data class Cast(
    val id: String,
    val name: String,
    val characterName: String?,
    val image: String?,
)