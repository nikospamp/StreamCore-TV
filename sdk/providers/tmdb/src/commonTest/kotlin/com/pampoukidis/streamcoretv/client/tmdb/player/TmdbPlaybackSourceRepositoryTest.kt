package com.pampoukidis.streamcoretv.client.tmdb.player

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbPlaybackSourceRepositoryTest {

    @Test
    fun moduleResolvesTheExistingPublicSintelDashSource() = runTest {
            val media = TmdbPlaybackSourceRepository().resolveSource(request())
            assertEquals("demo:sintel", media.assetId)
            assertEquals("Sintel (sample playback)", media.title)
            assertEquals("https://storage.googleapis.com/shaka-demo-assets/sintel/dash.mpd", media.uri)
            assertEquals("application/dash+xml", media.mimeType)
    }

    private fun request(): StreamCorePlaybackRequest {
        return StreamCorePlaybackRequest(
            profileId = "profile",
            contentId = "content-id",
            contentSnapshot = StreamCoreContent(
                id = "content-id",
                title = "Sintel",
                description = "",
                rating = 0,
                pgRatingName = "",
                pgRatingLevel = 0,
                poster = "",
                backdrop = null,
                cast = emptyList(),
                releaseDate = 0L,
                genres = emptyList(),
            ),
        )
    }
}
