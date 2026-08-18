package com.pampoukidis.streamcoretv.client.tmdb.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import javax.inject.Inject

internal class TmdbPlaybackSourceRepository @Inject constructor() : PlaybackSourceRepository {
    override suspend fun resolve(request: PlaybackRequestModel): PlaybackMediaModel {
        return PlaybackMediaModel(
            assetId = request.contentId,
            title = request.contentSnapshot.title,
            uri = DummyPlaybackUri,
            mimeType = DashMimeType,
        )
    }

    private companion object {
        const val DummyPlaybackUri = "https://storage.googleapis.com/shaka-demo-assets/sintel/dash.mpd"
        const val DashMimeType = "application/dash+xml"
    }
}