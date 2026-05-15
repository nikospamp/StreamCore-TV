package com.pampoukidis.streamcoretv.core.model.error

data class ErrorModel(
    val titleRes: Int,
    val messageRes: Int,
    val confirmActionRes: Int,
    val dismissible: Boolean = true,
)