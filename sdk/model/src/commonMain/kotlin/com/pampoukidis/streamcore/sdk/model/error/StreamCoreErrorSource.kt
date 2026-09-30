package com.pampoukidis.streamcore.sdk.model.error

/**
 * Optional diagnostics. Message text/backend codes are not a stable recovery or localized UI-wording API.
 */
data class StreamCoreErrorSource(
    val client: String? = null,
    val operation: String? = null,
    val httpCode: Int? = null,
    val backendCode: String? = null,
    val backendMessage: String? = null,
    val requestId: String? = null,
)
