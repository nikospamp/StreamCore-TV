package com.pampoukidis.streamcoretv.client.tmdb.player

import com.pampoukidis.streamcore.sdk.runtime.integration.provider.PlaybackProvider
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest

internal class TmdbPlaybackSourceRepository constructor() : PlaybackProvider {
    override suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCorePlaybackMedia {
        return StreamCorePlaybackMedia(
            assetId = "demo:sintel",
            title = "Sintel (sample playback)",
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
