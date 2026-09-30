package com.pampoukidis.streamcoretv.client.clientb.player

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientBPlaybackSourceRepositoryTest {

    @Test
    fun moduleResolvesTheExistingClientBDashSource() = runTest {
            val media = ClientBPlaybackSourceRepository().resolveSource(request())
            assertEquals("demo:trickplay", media.assetId)
            assertEquals("Trick-play demonstration (sample playback)", media.title)
            assertEquals(
                "https://raw.githubusercontent.com/rokudev/samples/0e7b37423132cddb1407489da7e05ceb23c35c56/media/TrickPlayThumbnailsDASH/master_multi.mpd",
                media.uri,
            )
            assertEquals("application/dash+xml", media.mimeType)
    }

    private fun request(): StreamCorePlaybackRequest {
        return StreamCorePlaybackRequest(
            profileId = "profile",
            contentId = "content-id",
            contentSnapshot = StreamCoreContent(
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
