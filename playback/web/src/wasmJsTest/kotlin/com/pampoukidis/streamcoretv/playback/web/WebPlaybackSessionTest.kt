package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class WebPlaybackSessionTest {
    @Test
    fun factoryCreatesDistinctSessionsWithSessionOwnedSurfaces() {
        val factory = WebPlaybackSessionFactory()

        val firstSession = factory.create()
        val secondSession = factory.create()

        assertTrue(firstSession !== secondSession)
        assertTrue(firstSession.videoSurface === firstSession.videoSurface)
        assertTrue(secondSession.videoSurface === secondSession.videoSurface)
        assertTrue(firstSession.videoSurface !== secondSession.videoSurface)

        firstSession.close()
        secondSession.close()
    }

    @Test
    fun commandsLeaveTheStubIdleAndNonPlaying() {
        val session = WebPlaybackSession()

        session.prepare(
            media = PlaybackMediaModel(
                assetId = "asset-id",
                title = "Title",
                uri = "https://example.invalid/stream.mpd",
                mimeType = "application/dash+xml",
            ),
            startPositionMillis = 12_345L,
        )
        session.play()
        session.pause()
        session.seekTo(54_321L)
        session.setSpeed(1.5f)
        session.selectVideoTrack("video-track")
        session.selectAudioTrack("audio-track")
        session.selectTextTrack("text-track")
        session.setResizeMode(PlaybackResizeMode.Fill)
        session.retry()

        assertEquals(PlaybackEngineState(), session.state.value)
    }

    @Test
    fun filmstripRequestsEmitNoFrames() {
        runTest {
            val session = WebPlaybackSession()

            val frames = session.requestFilmstrip(
                positionsMillis = listOf(0L, 5_000L, 10_000L),
            ).toList()

            assertTrue(frames.isEmpty())
        }
    }

    @Test
    fun closeIsIdempotentAndCommandsRemainSafe() {
        val session = WebPlaybackSession()

        session.close()
        session.close()
        session.play()
        session.seekTo(1_000L)
        session.retry()

        assertTrue(session.isClosed)
        assertEquals(PlaybackEngineState(), session.state.value)
    }
}
