package com.pampoukidis.streamcore.sdk.model.error

/**
 * One invalid input field and its reason, reported by an authoritative SDK operation.
 */
data class StreamCoreValidationIssue(val field: StreamCoreValidationField, val reason: StreamCoreValidationReason)
