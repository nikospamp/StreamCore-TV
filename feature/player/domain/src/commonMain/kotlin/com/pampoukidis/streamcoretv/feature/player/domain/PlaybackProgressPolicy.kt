package com.pampoukidis.streamcoretv.feature.player.domain

object PlaybackProgressPolicy {
    const val MinimumResumePositionMillis = 30_000L
    const val CompletionFraction = 0.95

    fun isResumable(positionMillis: Long, durationMillis: Long): Boolean {
        if (durationMillis <= 0L || positionMillis < MinimumResumePositionMillis) {
            return false
        }

        return positionMillis.toDouble() / durationMillis.toDouble() < CompletionFraction
    }
}