package com.pampoukidis.streamcore.sdk.runtime.playback

import com.pampoukidis.streamcore.sdk.runtime.error.ProviderOperationException

import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest

/**
 * Backend source resolution after runtime has resolved authoritative content and applied content policy.
 * Runtime owns progress recording and persistence; the application owns player rendering.
 * Throw [ProviderOperationException] for a typed backend failure; coroutine cancellation propagates.
 */
interface PlaybackProvider {
    suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCorePlaybackMedia
}
