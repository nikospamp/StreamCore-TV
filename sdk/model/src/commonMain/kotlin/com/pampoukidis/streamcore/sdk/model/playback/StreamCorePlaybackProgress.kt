package com.pampoukidis.streamcore.sdk.model.playback

import kotlinx.serialization.Serializable

@Serializable
data class StreamCorePlaybackProgress(
    val positionMillis: Long,
    val durationMillis: Long,
) {
    val fraction: Float
        get() {
            if (durationMillis <= 0L) {
                return 0f
            }

            return positionMillis
                .coerceIn(0L, durationMillis)
                .toFloat() / durationMillis.toFloat()
        }
}
