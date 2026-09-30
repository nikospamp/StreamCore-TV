package com.pampoukidis.streamcore.sdk.model.profile

/**
 * Provider-declared positive ASCII-digit count. Runtime validates shape; the provider verifies the PIN.
 * This contains neither the PIN nor attempt limits, recovery/creation rules or a persistent grant.
 */
data class StreamCoreProfilePinPolicy(val digitCount: Int) {
    init { require(digitCount > 0) }
}
