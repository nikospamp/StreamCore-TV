package com.pampoukidis.streamcoretv.client.tmdb.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository

internal class TmdbPlaybackSourceRepository constructor() : PlaybackSourceRepository {
    override suspend fun resolve(request: PlaybackRequestModel): PlaybackMediaModel {
        return PlaybackMediaModel(
            assetId = request.contentId,
            title = request.contentSnapshot.title,
            uri = DummyPlaybackUri,
            mimeType = DashMimeType,
        )
    }

    private companion object {
        // Stream with filmstrip data.
        // const val DummyPlaybackUri = "https://raw.githubusercontent.com/rokudev/samples/0e7b37423132cddb1407489da7e05ceb23c35c56/media/TrickPlayThumbnailsDASH/master_multi.mpd"
        const val DummyPlaybackUri = "https://storage.googleapis.com/shaka-demo-assets/sintel/dash.mpd"
        const val DashMimeType = "application/dash+xml"
    }
}