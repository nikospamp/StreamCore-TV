package com.pampoukidis.streamcore.sdk.model.error

/**
 * Structured authorization failure. Session restoration/login resolve AuthNotInitialized/Unauthenticated; entry resolves
 * NoActiveProfile. ProfileMismatch requires the active profile ID; ProfileUnavailable requires a fresh list.
 * StaleSession/StaleActivation reject prior work; create new operations and observers after re-entry.
 * PinChallengeExpired requires a new selection/challenge rather than resubmitting an old challenge ID.
 */
enum class StreamCoreContextFailureReason {
    AuthNotInitialized, Unauthenticated, NoActiveProfile, ProfileMismatch, ProfileUnavailable,
    StaleSession, StaleActivation, PinChallengeExpired,
}
