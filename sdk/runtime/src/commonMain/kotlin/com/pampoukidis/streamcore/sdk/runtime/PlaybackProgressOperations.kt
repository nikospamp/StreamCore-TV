package com.pampoukidis.streamcore.sdk.runtime

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import kotlinx.coroutines.flow.Flow

/** Progress-only operations bound to the activation captured by the runtime facade. */
internal interface PlaybackProgressOperations {
    fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>>

    /** Applies resume policy to a timestamped snapshot, saving or removing its progress. */
    suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit>

    suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit>
}
