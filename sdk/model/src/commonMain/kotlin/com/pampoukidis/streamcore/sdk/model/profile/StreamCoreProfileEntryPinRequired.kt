package com.pampoukidis.streamcore.sdk.model.profile

/**
 * Selection awaits provider PIN verification. Submit the challenge ID, or cancel it when leaving the PIN flow.
 */
data class StreamCoreProfileEntryPinRequired(val challenge: StreamCoreProfilePinChallenge) : StreamCoreProfileEntryResult, StreamCoreProfileSelectionResult
