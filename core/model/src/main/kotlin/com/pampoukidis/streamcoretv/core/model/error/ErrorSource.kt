package com.pampoukidis.streamcoretv.core.model.error

data class ErrorSource(
    val client: String? = null,
    val operation: String? = null,
    val httpCode: Int? = null,
    val backendCode: String? = null,
    val backendMessage: String? = null,
    val requestId: String? = null,
)