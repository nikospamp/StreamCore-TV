package com.pampoukidis.streamcoretv.core.model.auth

data class ProfileParentalLevelModel(
    val id: String,
    val label: String,
    val rank: Int,
    val isKids: Boolean = false,
)
