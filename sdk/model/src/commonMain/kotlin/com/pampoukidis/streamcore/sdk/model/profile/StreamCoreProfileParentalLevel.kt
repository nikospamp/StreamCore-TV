package com.pampoukidis.streamcore.sdk.model.profile

data class StreamCoreProfileParentalLevel(
    val id: String,
    val label: String,
    val rank: Int,
    val isKids: Boolean = false,
)
