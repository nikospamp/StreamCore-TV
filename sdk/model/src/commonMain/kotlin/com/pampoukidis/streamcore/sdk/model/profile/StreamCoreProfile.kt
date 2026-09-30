package com.pampoukidis.streamcore.sdk.model.profile

/**
 * Provider-neutral profile metadata. Owning or listing this value does not authorize its content operations.
 * `isKidsProfile` reflects provider policy; numeric parental levels are not a universal age rule.
 */
data class StreamCoreProfile(
    val id: String,
    val displayName: String,
    val avatar: StreamCoreProfileAvatar,
    val parentalLevel: StreamCoreProfileParentalLevel,
    /** Provider permission applied by account-scoped profile management. */
    val canDelete: Boolean,
    val isKidsProfile: Boolean,
    /** Null means selection needs no PIN; otherwise runtime requests provider verification. */
    val pinPolicy: StreamCoreProfilePinPolicy? = null,
)
