package com.pampoukidis.streamcore.sdk.model.profile

/**
 * Opaque challenge bound to one SDK instance, account and entry attempt. Contains neither PIN nor authorization.
 * digitCount describes ASCII numeric input; the provider verifies it. Keep draft PINs in transient UI state.
 * Selection, cancellation, logout or close can expire it; handle the confirming operation's typed result.
 */
data class StreamCoreProfilePinChallenge(
    val challengeId: String,
    val profile: StreamCoreProfile,
    val digitCount: Int,
)
