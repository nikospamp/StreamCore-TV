package com.pampoukidis.streamcoretv.web.playback

import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest

/** Overrides diagnostic media; progress retains SDK-owned authorization and storage behavior. */
internal class DiagnosticPlaybackService(delegate: PlaybackService) : PlaybackService by delegate {
    override suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCoreResult<StreamCorePlaybackMedia> {
        return StreamCoreResult.Success(
            StreamCorePlaybackMedia(
                assetId = request.contentId,
                title = request.contentSnapshot.title,
                uri = null,
            ),
        )
    }
}
