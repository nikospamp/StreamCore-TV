package com.pampoukidis.streamcore.sdk.model.catalog

import kotlinx.serialization.Serializable

@Serializable
data class StreamCoreCastMember(
    val id: String,
    val name: String,
    val characterName: String?,
    val image: String?,
)
