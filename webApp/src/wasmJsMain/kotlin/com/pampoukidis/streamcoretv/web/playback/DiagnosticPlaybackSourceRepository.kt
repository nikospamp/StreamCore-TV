package com.pampoukidis.streamcoretv.web.playback

import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository

internal object DiagnosticPlaybackSourceRepository : PlaybackSourceRepository {
    override suspend fun resolve(request: PlaybackRequestModel): PlaybackMediaModel {
        return PlaybackMediaModel(
            assetId = request.contentId,
            title = request.contentSnapshot.title,
            uri = null,
        )
    }
}
