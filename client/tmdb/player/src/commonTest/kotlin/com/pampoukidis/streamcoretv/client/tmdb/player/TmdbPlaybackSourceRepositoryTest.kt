package com.pampoukidis.streamcoretv.client.tmdb.player

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbPlaybackSourceRepositoryTest {

    @Test
    fun moduleResolvesTheExistingPublicSintelDashSource() = runTest {
        val koin = startKoin { modules(tmdbPlayerModule) }.koin
        try {
            val media = koin.get<PlaybackSourceRepository>().resolve(request())
            assertEquals("content-id", media.assetId)
            assertEquals("Sintel", media.title)
            assertEquals("https://storage.googleapis.com/shaka-demo-assets/sintel/dash.mpd", media.uri)
            assertEquals("application/dash+xml", media.mimeType)
        } finally {
            stopKoin()
        }
    }

    private fun request(): PlaybackRequestModel {
        return PlaybackRequestModel(
            profileId = "profile",
            contentId = "content-id",
            contentSnapshot = ContentModel(
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
