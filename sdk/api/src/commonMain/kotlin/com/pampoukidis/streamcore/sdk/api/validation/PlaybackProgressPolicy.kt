package com.pampoukidis.streamcore.sdk.api.validation

/** Pure resume eligibility feedback. Runtime progress operations remain responsible for enforcing this policy. */
object PlaybackProgressPolicy {
    const val MinimumResumePositionMillis = 30_000L
    const val CompletionFraction = 0.95

    /** Requires positive duration, position at least 30 seconds, and completion strictly below 95 percent. */
    fun isResumable(positionMillis: Long, durationMillis: Long): Boolean {
        if (durationMillis <= 0L || positionMillis < MinimumResumePositionMillis) {
            return false
        }

        return positionMillis.toDouble() / durationMillis.toDouble() < CompletionFraction
    }
}