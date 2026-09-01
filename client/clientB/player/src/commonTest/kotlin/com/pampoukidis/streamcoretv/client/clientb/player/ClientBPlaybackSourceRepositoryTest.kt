package com.pampoukidis.streamcoretv.client.clientb.player

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientBPlaybackSourceRepositoryTest {

    @Test
    fun moduleResolvesTheExistingClientBDashSource() = runTest {
        val koin = startKoin { modules(clientBPlayerModule) }.koin
        try {
            val media = koin.get<PlaybackSourceRepository>().resolve(request())
            assertEquals("content-id", media.assetId)
            assertEquals("Client B", media.title)
            assertEquals(
                "https://raw.githubusercontent.com/rokudev/samples/0e7b37423132cddb1407489da7e05ceb23c35c56/media/TrickPlayThumbnailsDASH/master_multi.mpd",
                media.uri,
            )
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
                title = "Client B",
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
