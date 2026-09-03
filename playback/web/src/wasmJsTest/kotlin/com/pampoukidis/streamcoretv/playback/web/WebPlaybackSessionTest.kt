package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackTrackType
import kotlinx.browser.document
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLVideoElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class WebPlaybackSessionTest {
    @Test
    fun prepareAndCommandsDriveTheBridgeAndMapState() {
        val fixture = createFixture()

        fixture.session.prepare(media(), startPositionMillis = 12_345L)

        assertEquals(PlaybackPhase.Preparing, fixture.session.state.value.phase)
        assertEquals(1, fixture.bridge.lastLoadGeneration)
        assertEquals("https://media.example/stream.mpd", fixture.bridge.lastLoadUri)
        assertEquals("application/dash+xml", fixture.bridge.lastLoadMimeType)
        assertEquals(12_345L, fixture.bridge.lastLoadStartPositionMillis)

        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Buffering,
                positionMillis = 12_500L,
                durationMillis = 60_000L,
                bufferedPositionMillis = 30_000L,
                videoAspectRatio = 16f / 9f,
            ),
        )
        assertEquals(PlaybackPhase.Buffering, fixture.session.state.value.phase)
        assertEquals(30_000L, fixture.session.state.value.bufferedPositionMillis)

        fixture.session.play()
        fixture.session.pause()
        fixture.session.seekTo(90_000L)
        fixture.session.setSpeed(3f)
        fixture.session.setResizeMode(PlaybackResizeMode.Fill)
        fixture.session.selectVideoTrack("video:1")
        fixture.session.selectAudioTrack("audio:2")
        fixture.session.selectTextTrack(null)
        fixture.session.selectVideoTrack(null)
        fixture.session.selectAudioTrack(null)

        assertEquals(1, fixture.bridge.playCount)
        assertEquals(1, fixture.bridge.pauseCount)
        assertEquals(listOf(60_000L), fixture.bridge.seekPositionsMillis)
        assertEquals(listOf(2f), fixture.bridge.speeds)
        assertEquals(listOf(PlaybackResizeMode.Fill), fixture.bridge.resizeModes)
        assertEquals(listOf("video:1", null), fixture.bridge.videoTrackSelections)
        assertEquals(listOf("audio:2", null), fixture.bridge.audioTrackSelections)
        assertEquals(listOf<String?>(null), fixture.bridge.textTrackSelections)
        assertEquals(2f, fixture.session.state.value.speed)
        assertEquals(PlaybackResizeMode.Fill, fixture.session.state.value.resizeMode)
    }

    @Test
    fun autoplayRejectionIsReadyAndPausedWithoutAnError() {
        val fixture = createFixture()
        fixture.session.prepare(media(), 0L)

        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ready,
                isPlaying = false,
                durationMillis = 60_000L,
            ),
        )

        assertEquals(PlaybackPhase.Ready, fixture.session.state.value.phase)
        assertFalse(fixture.session.state.value.isPlaying)
        assertNull(fixture.session.state.value.error)
    }

    @Test
    fun bridgeFailureMapsToFixedSanitizedCopy() {
        val fixture = createFixture()
        fixture.session.prepare(
            media = media(uri = "https://media.example/stream.mpd?token=secret-value"),
            startPositionMillis = 0L,
        )

        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Error,
                failure = WebPlaybackFailure(isRecoverable = false),
            ),
        )

        val error = requireNotNull(fixture.session.state.value.error)
        assertEquals("PLAYBACK_FAILED", error.code)
        assertEquals("Playback failed.", error.message)
        assertFalse(error.isRecoverable)
        assertFalse(error.code.contains("token", ignoreCase = true))
        assertFalse(error.message.contains("secret-value"))
    }

    @Test
    fun missingSourceUsesFixedSourceErrorWithoutLoadingTheBridge() {
        val fixture = createFixture()

        fixture.session.prepare(media(uri = null), 0L)

        assertEquals(PlaybackPhase.Error, fixture.session.state.value.phase)
        assertEquals("SOURCE_MISSING", fixture.session.state.value.error?.code)
        assertEquals("No playable source is available.", fixture.session.state.value.error?.message)
        assertEquals(0, fixture.bridge.loadCount)
        assertEquals(1, fixture.bridge.resetCount)
        assertEquals(1, fixture.bridge.lastResetGeneration)

        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ready,
                isPlaying = true,
            ),
        )
        assertEquals(PlaybackPhase.Error, fixture.session.state.value.phase)
    }

    @Test
    fun trackSnapshotsMapWithoutBrowserOrShakaTypes() {
        val fixture = createFixture()
        fixture.session.prepare(media(), 0L)

        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ready,
                videoTracks = listOf(
                    WebPlaybackTrackSnapshot(
                        id = "video:1080",
                        type = PlaybackTrackType.Video,
                        label = "1080p",
                        videoHeight = 1080,
                    ),
                ),
                audioTracks = listOf(
                    WebPlaybackTrackSnapshot(
                        id = "audio:el",
                        type = PlaybackTrackType.Audio,
                        label = "Greek",
                        language = "el",
                    ),
                ),
                textTracks = listOf(
                    WebPlaybackTrackSnapshot(
                        id = "text:en",
                        type = PlaybackTrackType.Text,
                        label = "English",
                        language = "en",
                    ),
                ),
                selectedVideoTrackId = "video:1080",
                selectedAudioTrackId = "audio:el",
                selectedTextTrackId = "text:en",
            ),
        )

        assertEquals(
            listOf(
                PlaybackTrackModel(
                    id = "video:1080",
                    type = PlaybackTrackType.Video,
                    label = "1080p",
                    videoHeight = 1080,
                ),
            ),
            fixture.session.state.value.videoTracks,
        )
        assertEquals("audio:el", fixture.session.state.value.selectedAudioTrackId)
        assertEquals("text:en", fixture.session.state.value.selectedTextTrackId)
    }

    @Test
    fun videoAutoReportsNoVideoSelectionWhileAudioRemainsExplicit() {
        val fixture = createFixture()
        fixture.session.prepare(media(), 0L)
        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ready,
                selectedVideoTrackId = "video:1080",
                selectedAudioTrackId = "audio:el",
            ),
        )

        fixture.session.selectVideoTrack(null)
        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ready,
                selectedVideoTrackId = null,
                selectedAudioTrackId = "audio:el",
            ),
        )

        assertEquals(listOf<String?>(null), fixture.bridge.videoTrackSelections)
        assertNull(fixture.session.state.value.selectedVideoTrackId)
        assertEquals("audio:el", fixture.session.state.value.selectedAudioTrackId)
    }

    @Test
    fun stalePrepareGenerationCannotReplaceCurrentMediaState() {
        val fixture = createFixture()
        fixture.session.prepare(media(assetId = "first"), 0L)
        fixture.session.prepare(media(assetId = "second"), 20_000L)

        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ended,
                durationMillis = 10L,
            ),
        )

        assertEquals(PlaybackPhase.Preparing, fixture.session.state.value.phase)

        fixture.bridge.emit(
            generation = 2,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ready,
                durationMillis = 80_000L,
            ),
        )
        assertEquals(PlaybackPhase.Ready, fixture.session.state.value.phase)
        assertEquals(80_000L, fixture.session.state.value.durationMillis)
    }

    @Test
    fun endedPlayRewindsBeforePlaying() {
        val fixture = createFixture()
        fixture.session.prepare(media(), 0L)
        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ended,
                positionMillis = 60_000L,
                durationMillis = 60_000L,
            ),
        )

        fixture.session.play()

        assertEquals(listOf(0L), fixture.bridge.seekPositionsMillis)
        assertEquals(1, fixture.bridge.playCount)
    }

    @Test
    fun retryCreatesANewGuardedLoadAtTheLastKnownPosition() {
        val fixture = createFixture()
        fixture.session.prepare(media(), 4_000L)
        fixture.bridge.emit(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Error,
                positionMillis = 8_000L,
                failure = WebPlaybackFailure(isRecoverable = true),
            ),
        )

        fixture.session.retry()

        assertEquals(2, fixture.bridge.lastLoadGeneration)
        assertEquals(8_000L, fixture.bridge.lastLoadStartPositionMillis)
        assertEquals(PlaybackPhase.Preparing, fixture.session.state.value.phase)
    }

    @Test
    fun manifestFilmstripFramesDecodeToComposeImageBitmaps() {
        runTest {
            val fixture = createFixture()
            fixture.session.prepare(media(), 0L)
            fixture.bridge.filmstripFrames = listOf(
                WebFilmstripFrameSnapshot(
                    positionMillis = 5_000L,
                    encodedPng = OnePixelPng,
                ),
            )

            val frames = fixture.session.requestFilmstrip(
                positionsMillis = listOf(-1L, 5_000L, 5_000L),
            ).toList()

            assertEquals(listOf(0L, 5_000L), fixture.bridge.lastFilmstripPositionsMillis)
            assertEquals(1, fixture.bridge.lastFilmstripGeneration)
            assertEquals(1, frames.size)
            assertEquals(5_000L, frames.single().positionMillis)
            val image = assertNotNull(frames.single().image)
            assertEquals(1, image.width)
            assertEquals(1, image.height)
            assertEquals(1, fixture.bridge.filmstripCancelCount)
        }
    }

    @Test
    fun filmstripRequestsEmitNoFramesWhenManifestHasNoImageTrack() {
        runTest {
            val fixture = createFixture()
            fixture.session.prepare(media(), 0L)

            val frames = fixture.session.requestFilmstrip(
                positionsMillis = listOf(0L, 5_000L, 10_000L),
            ).toList()

            assertTrue(frames.isEmpty())
            assertEquals(1, fixture.bridge.filmstripCancelCount)
        }
    }

    @Test
    fun closeCompletesPendingFilmstripAndReleasesItsCallback() {
        runTest {
            val fixture = createFixture()
            fixture.session.prepare(media(), 0L)
            fixture.bridge.completeFilmstripRequestsImmediately = false
            val frames = async {
                fixture.session.requestFilmstrip(listOf(5_000L)).toList()
            }
            runCurrent()

            fixture.session.close()

            assertTrue(frames.await().isEmpty())
            assertEquals(1, fixture.bridge.filmstripCancelCount)
        }
    }

    @Test
    fun closeIsIdempotentDetachesCallbacksAndRejectsLateWork() {
        val fixture = createFixture()
        fixture.session.prepare(media(), 0L)

        fixture.session.close()
        fixture.session.close()
        fixture.session.play()
        fixture.session.seekTo(1_000L)
        fixture.session.retry()
        fixture.bridge.emitLate(
            generation = 1,
            snapshot = WebPlaybackSnapshot(
                phase = WebPlaybackPhase.Ready,
                isPlaying = true,
            ),
        )

        assertTrue(fixture.session.isClosed)
        assertEquals(1, fixture.bridge.listenerAttachCount)
        assertEquals(1, fixture.bridge.listenerDetachCount)
        assertEquals(1, fixture.bridge.closeCount)
        assertEquals(0, fixture.bridge.playCount)
        assertTrue(fixture.bridge.seekPositionsMillis.isEmpty())
        assertEquals(PlaybackEngineState(), fixture.session.state.value)
    }

    private fun createFixture(): Fixture {
        val bridge = FakeWebPlaybackBridge()
        val videoElement = document.createElement("video") as HTMLVideoElement
        val surface = WebPlaybackVideoSurface(videoElement)
        return Fixture(
            bridge = bridge,
            session = WebPlaybackSession(bridge, surface),
        )
    }

    private fun media(
        assetId: String = "asset-id",
        uri: String? = "https://media.example/stream.mpd",
    ): PlaybackMediaModel {
        return PlaybackMediaModel(
            assetId = assetId,
            title = "Title",
            uri = uri,
            mimeType = "application/dash+xml",
        )
    }

    private data class Fixture(
        val bridge: FakeWebPlaybackBridge,
        val session: WebPlaybackSession,
    )

    private companion object {
        const val OnePixelPng =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    }
}
