package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.lifecycle.ViewModelStore
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackMediaModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackResizeMode
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUpDispatcher() {
        mainDispatcherRule.setUp()
    }

    @AfterTest
    fun tearDownDispatcher() {
        mainDispatcherRule.tearDown()
    }

    @Test
    fun `load resolves source prepares one session at persisted position`() = runTest {
        val progress = FakeProgressRepository().apply { current = progressEntry(position = 45_000L) }
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(
            sourceRepository = FakeSourceRepository(),
            progressRepository = progress,
            sessionFactory = factory,
            clock = PlayerClock { 1_234L },
        )

        subject.onAction(PlayerAction.Load(request(), isPipSupported = true))
        runCurrent()

        assertEquals(1, factory.sessions.size)
        assertEquals(45_000L, factory.sessions.single().preparedAt)
        assertNotNull(subject.videoSurface.value)
        assertTrue(subject.uiState.value.isPipSupported)
    }

    @Test
    fun `commands are forwarded and scrub seeks only on finish`() = runTest {
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), FakeProgressRepository(), factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        val session = factory.sessions.single()
        session.emit(PlaybackEngineState(phase = PlaybackPhase.Ready, isPlaying = true, positionMillis = 50_000L, durationMillis = 100_000L))
        runCurrent()

        subject.onAction(PlayerAction.ScrubStarted)
        subject.onAction(PlayerAction.ScrubChanged(70_000L))
        assertEquals(emptyList<Long>(), session.seeks)
        assertEquals(1, session.pauseCount)

        subject.onAction(PlayerAction.ScrubFinished)
        runCurrent()
        assertEquals(listOf(70_000L), session.seeks)
        assertEquals(1, session.playCount)

        subject.onAction(PlayerAction.SelectSpeed(1.5f))
        subject.onAction(PlayerAction.SelectResizeMode(PlaybackResizeMode.Fill))
        assertEquals(1.5f, session.selectedSpeed)
        assertEquals(PlaybackResizeMode.Fill, session.selectedResizeMode)
    }

    @Test
    fun `rapid scrubbing conflates pending filmstrip targets`() = runTest {
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), FakeProgressRepository(), factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        val session = factory.sessions.single()
        session.emit(PlaybackEngineState(phase = PlaybackPhase.Ready, durationMillis = 120_000L, positionMillis = 40_000L))
        runCurrent()

        subject.onAction(PlayerAction.ScrubStarted)
        runCurrent()
        subject.onAction(PlayerAction.ScrubChanged(50_000L))
        subject.onAction(PlayerAction.ScrubChanged(70_000L))
        advanceTimeBy(1L)
        runCurrent()

        assertEquals(
            listOf(
                listOf(40_000L, 35_000L, 45_000L, 30_000L, 50_000L),
                listOf(70_000L, 65_000L, 75_000L, 60_000L, 80_000L),
            ),
            session.filmstripRequests,
        )
        assertEquals(listOf(40_000L), session.filmstripEmissions)

        advanceTimeBy(1L)
        runCurrent()

        assertNotNull(subject.uiState.value.filmstripFrames[2].image)
        assertEquals(70_000L, subject.uiState.value.scrubPositionMillis)
    }

    @Test
    fun `filmstrip frames are rendered progressively`() = runTest {
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), FakeProgressRepository(), factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        val session = factory.sessions.single()
        session.emit(PlaybackEngineState(phase = PlaybackPhase.Ready, durationMillis = 120_000L, positionMillis = 40_000L))
        runCurrent()

        subject.onAction(PlayerAction.ScrubStarted)
        val placeholders = subject.uiState.value.filmstripFrames
        runCurrent()
        advanceTimeBy(1L)
        runCurrent()
        val centerLoaded = subject.uiState.value.filmstripFrames

        assertNotSame(placeholders, centerLoaded)
        assertEquals(1, centerLoaded.count { frame -> frame.image != null })
        assertEquals(listOf(40_000L), session.filmstripEmissions)

        advanceTimeBy(1L)
        runCurrent()
        val secondFrameLoaded = subject.uiState.value.filmstripFrames

        assertNotSame(centerLoaded, secondFrameLoaded)
        assertEquals(2, secondFrameLoaded.count { frame -> frame.image != null })
        assertEquals(listOf(40_000L, 35_000L), session.filmstripEmissions)

        advanceTimeBy(3L)
        runCurrent()

        assertEquals(5, subject.uiState.value.filmstripFrames.count { frame -> frame.image != null })
    }

    @Test
    fun `retry re-resolves source and reuses session for subsequent loads`() = runTest {
        val source = FakeSourceRepository(fail = true)
        val progress = FakeProgressRepository().apply { current = progressEntry(position = 42_000L) }
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(source, progress, factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()

        assertEquals(PlaybackPhase.Error, subject.uiState.value.phase)
        assertEquals(1, source.resolvedRequests.size)
        assertEquals(1, factory.sessions.size)

        source.fail = false
        subject.onAction(PlayerAction.Retry)
        runCurrent()

        subject.onAction(PlayerAction.Load(request(contentId = "next-content"), false))
        runCurrent()

        assertEquals(
            listOf("content", "content", "next-content"),
            source.resolvedRequests.map { request -> request.contentId },
        )
        assertEquals(1, factory.sessions.size)
        assertEquals(listOf("content", "next-content"), factory.sessions.single().preparedMediaIds)
        assertEquals(listOf(42_000L, 42_000L), factory.sessions.single().preparedPositions)
    }

    @Test
    fun `source resolution failure exposes only sanitized copy`() = runTest {
        val secret = "super-secret-token"
        val source = FakeSourceRepository(
            fail = true,
            failureMessage = "Request failed: https://example.test/video.mpd?access_token=$secret",
        )
        val subject = PlayerViewModel(source, FakeProgressRepository(), FakeSessionFactory())

        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()

        assertEquals("Unable to load this video.", subject.uiState.value.error?.message)
        assertFalse(subject.uiState.value.toString().contains(secret))
    }

    @Test
    fun `clearing view model closes its single session exactly once`() = runTest {
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), FakeProgressRepository(), factory)
        val store = ViewModelStore()
        store.put("player", subject)

        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        store.clear()
        store.clear()

        assertEquals(1, factory.sessions.size)
        assertEquals(1, factory.sessions.single().closeCount)
        assertEquals(null, subject.videoSurface.value)
    }

    @Test
    fun `pause persists only eligible progress`() = runTest {
        val progress = FakeProgressRepository()
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(
            sourceRepository = FakeSourceRepository(),
            progressRepository = progress,
            sessionFactory = factory,
            clock = PlayerClock { 1_234L },
        )
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        val session = factory.sessions.single()

        session.emit(PlaybackEngineState(phase = PlaybackPhase.Ready, isPlaying = true, positionMillis = 29_000L, durationMillis = 100_000L))
        runCurrent()
        subject.onAction(PlayerAction.TogglePlayPause)
        runCurrent()
        assertTrue(progress.removeCount >= 1)
        assertEquals(0, progress.upserts.size)

        session.emit(PlaybackEngineState(phase = PlaybackPhase.Ready, isPlaying = true, positionMillis = 31_000L, durationMillis = 100_000L))
        runCurrent()
        subject.onAction(PlayerAction.TogglePlayPause)
        runCurrent()
        assertEquals(31_000L, progress.upserts.last().positionMillis)
        assertEquals(1_234L, progress.upserts.last().updatedAtMillis)
    }

    @Test
    fun `back before duration is known preserves existing resume progress`() = runTest {
        val progress = FakeProgressRepository().apply {
            current = progressEntry(position = 45_000L)
        }
        val subject = PlayerViewModel(
            sourceRepository = FakeSourceRepository(),
            progressRepository = progress,
            sessionFactory = FakeSessionFactory(),
        )
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()

        subject.onAction(PlayerAction.BackSelected)
        runCurrent()

        assertEquals(0, progress.removeCount)
        assertEquals(0, progress.upsertAttemptCount)
        assertEquals(PlayerEffect.NavigateBack, subject.effects.first())
    }

    @Test
    fun `back navigates once when progress persistence fails`() = runTest {
        val progress = FakeProgressRepository()
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), progress, factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        factory.sessions.single().emit(
            PlaybackEngineState(
                phase = PlaybackPhase.Ready,
                positionMillis = 45_000L,
                durationMillis = 100_000L,
            ),
        )
        runCurrent()
        progress.failWrites = true

        subject.onAction(PlayerAction.BackSelected)
        subject.onAction(PlayerAction.BackSelected)
        runCurrent()

        assertEquals(1, progress.upsertAttemptCount)
        assertEquals(PlayerEffect.NavigateBack, subject.effects.first())
    }

    @Test
    fun `back closes settings without persisting or navigating`() = runTest {
        val progress = FakeProgressRepository()
        val subject = PlayerViewModel(FakeSourceRepository(), progress, FakeSessionFactory())
        val effects = mutableListOf<PlayerEffect>()
        backgroundScope.launch { subject.effects.collect(effects::add) }
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        subject.onAction(PlayerAction.OpenSettings())

        subject.onAction(PlayerAction.BackSelected)
        runCurrent()

        assertEquals(null, subject.uiState.value.settingsPage)
        assertEquals(0, progress.upsertAttemptCount)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `playing engine updates do not postpone controls auto hide`() = runTest {
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), FakeProgressRepository(), factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        val session = factory.sessions.single()
        val playingState = PlaybackEngineState(
            phase = PlaybackPhase.Ready,
            isPlaying = true,
            durationMillis = 100_000L,
        )

        session.emit(playingState)
        runCurrent()
        repeat(9) { second ->
            advanceTimeBy(1_000L)
            session.emit(playingState.copy(positionMillis = (second + 1) * 1_000L))
            runCurrent()
            assertTrue(subject.uiState.value.controlsVisible)
        }

        advanceTimeBy(1_000L)
        runCurrent()

        assertFalse(subject.uiState.value.controlsVisible)
    }

    @Test
    fun `explicit user interaction restarts controls auto hide`() = runTest {
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), FakeProgressRepository(), factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        factory.sessions.single().emit(
            PlaybackEngineState(
                phase = PlaybackPhase.Ready,
                isPlaying = true,
                durationMillis = 100_000L,
            ),
        )
        runCurrent()

        advanceTimeBy(9_000L)
        subject.onAction(PlayerAction.UserInteraction)
        advanceTimeBy(9_000L)
        runCurrent()

        assertTrue(subject.uiState.value.controlsVisible)

        advanceTimeBy(1_000L)
        runCurrent()

        assertFalse(subject.uiState.value.controlsVisible)
    }

    private fun request(contentId: String = "content"): PlaybackRequestModel {
        return PlaybackRequestModel("profile", contentId, content(contentId))
    }

    private fun progressEntry(position: Long): PlaybackProgressEntryModel {
        return PlaybackProgressEntryModel("profile", "content", content(), position, 100_000L, 1L)
    }

    private fun content(contentId: String = "content"): ContentModel {
        return ContentModel(contentId, "Title", "", 0, "", 0, "", null, emptyList(), 0L, emptyList())
    }

    private class FakeSourceRepository(
        var fail: Boolean = false,
        private val failureMessage: String = "source failed",
    ) : PlaybackSourceRepository {
        val resolvedRequests = mutableListOf<PlaybackRequestModel>()

        override suspend fun resolve(request: PlaybackRequestModel): PlaybackMediaModel {
            resolvedRequests += request
            if (fail) {
                error(failureMessage)
            }
            return PlaybackMediaModel(request.contentId, request.contentSnapshot.title, "https://example.test/video.mpd", "application/dash+xml")
        }
    }

    private class FakeProgressRepository : PlaybackProgressRepository {
        var current: PlaybackProgressEntryModel? = null
        val upserts = mutableListOf<PlaybackProgressEntryModel>()
        var removeCount = 0
        var upsertAttemptCount = 0
        var failWrites = false
        override fun observe(profileId: String): Flow<List<PlaybackProgressEntryModel>> {
            return MutableStateFlow(current?.let(::listOf).orEmpty())
        }

        override suspend fun get(profileId: String, contentId: String): PlaybackProgressEntryModel? = current
        override suspend fun upsert(entry: PlaybackProgressEntryModel) {
            upsertAttemptCount += 1
            if (failWrites) {
                error("progress write failed")
            }
            upserts += entry
        }

        override suspend fun remove(profileId: String, contentId: String) {
            removeCount += 1
            if (failWrites) {
                error("progress write failed")
            }
        }
    }

    private class FakeSessionFactory : PlaybackSessionFactory {
        val sessions = mutableListOf<FakeSession>()
        override fun create(): PlaybackSession {
            return FakeSession().also(sessions::add)
        }
    }

    private class FakeSession : PlaybackSession {
        private val mutableState = MutableStateFlow(PlaybackEngineState())
        override val state: StateFlow<PlaybackEngineState> = mutableState
        override val videoSurface: PlaybackVideoSurface = FakeSurface
        var preparedAt: Long? = null
        val preparedMediaIds = mutableListOf<String>()
        val preparedPositions = mutableListOf<Long>()
        var playCount = 0
        var pauseCount = 0
        var closeCount = 0
        val seeks = mutableListOf<Long>()
        var selectedSpeed = 1f
        var selectedResizeMode = PlaybackResizeMode.Fit
        val filmstripRequests = mutableListOf<List<Long>>()
        val filmstripEmissions = mutableListOf<Long>()
        fun emit(value: PlaybackEngineState) {
            mutableState.value = value
        }

        override fun prepare(media: PlaybackMediaModel, startPositionMillis: Long) {
            preparedAt = startPositionMillis
            preparedMediaIds += media.assetId
            preparedPositions += startPositionMillis
        }

        override fun play() {
            playCount += 1
        }

        override fun pause() {
            pauseCount += 1
        }

        override fun seekTo(positionMillis: Long) {
            seeks += positionMillis
        }

        override fun setSpeed(speed: Float) {
            selectedSpeed = speed
        }

        override fun selectVideoTrack(trackId: String?) = Unit
        override fun selectAudioTrack(trackId: String?) = Unit
        override fun selectTextTrack(trackId: String?) = Unit
        override fun setResizeMode(mode: PlaybackResizeMode) {
            selectedResizeMode = mode
        }

        override fun retry() = Unit
        override fun requestFilmstrip(positionsMillis: List<Long>): Flow<PlaybackFilmstripFrameModel> {
            filmstripRequests += positionsMillis
            return flow {
                positionsMillis.forEach { positionMillis ->
                    delay(1L)
                    filmstripEmissions += positionMillis
                    emit(PlaybackFilmstripFrameModel(positionMillis, FakeImageBitmap))
                }
            }
        }

        override fun close() {
            closeCount += 1
        }
    }

    private object FakeSurface : PlaybackVideoSurface {
        @Composable
        override fun Render(modifier: Modifier) = Unit
    }

    private object FakeImageBitmap : ImageBitmap {
        override val width = 1
        override val height = 1
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override val hasAlpha = false
        override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888

        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) {
            return
        }

        override fun prepareToDraw() {
            return
        }
    }
}
