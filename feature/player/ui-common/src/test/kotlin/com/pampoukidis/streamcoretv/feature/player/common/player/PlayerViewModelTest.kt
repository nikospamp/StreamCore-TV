package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `load resolves source prepares one session at persisted position`() = runTest {
        val progress = FakeProgressRepository().apply { current = progressEntry(position = 45_000L) }
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), progress, factory)

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
    fun `stale filmstrip debounce is cancelled`() = runTest {
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), FakeProgressRepository(), factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        val session = factory.sessions.single()
        session.emit(PlaybackEngineState(phase = PlaybackPhase.Ready, durationMillis = 120_000L, positionMillis = 40_000L))
        runCurrent()

        subject.onAction(PlayerAction.ScrubStarted)
        subject.onAction(PlayerAction.ScrubChanged(50_000L))
        advanceTimeBy(100L)
        subject.onAction(PlayerAction.ScrubChanged(70_000L))
        advanceTimeBy(151L)
        runCurrent()

        assertEquals(1, session.filmstripRequests.size)
        assertEquals(70_000L, session.filmstripRequests.single()[2])
    }

    @Test
    fun `retry recreates failed source session at last position`() = runTest {
        val source = FakeSourceRepository(fail = true)
        val progress = FakeProgressRepository().apply { current = progressEntry(position = 42_000L) }
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(source, progress, factory)
        subject.onAction(PlayerAction.Load(request(), false))
        runCurrent()
        assertEquals(PlaybackPhase.Error, subject.uiState.value.phase)

        source.fail = false
        subject.onAction(PlayerAction.Retry)
        runCurrent()
        assertEquals(42_000L, factory.sessions.single().preparedAt)
    }

    @Test
    fun `pause persists only eligible progress`() = runTest {
        val progress = FakeProgressRepository()
        val factory = FakeSessionFactory()
        val subject = PlayerViewModel(FakeSourceRepository(), progress, factory)
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

    private fun request(): PlaybackRequestModel {
        return PlaybackRequestModel("profile", "content", content())
    }

    private fun progressEntry(position: Long): PlaybackProgressEntryModel {
        return PlaybackProgressEntryModel("profile", "content", content(), position, 100_000L, 1L)
    }

    private fun content(): ContentModel {
        return ContentModel("content", "Title", "", 0, "", 0, "", null, emptyList(), 0L, emptyList())
    }

    private class FakeSourceRepository(var fail: Boolean = false) : PlaybackSourceRepository {
        override suspend fun resolve(request: PlaybackRequestModel): PlaybackMediaModel {
            if (fail) error("source failed")
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
        var playCount = 0
        var pauseCount = 0
        val seeks = mutableListOf<Long>()
        var selectedSpeed = 1f
        var selectedResizeMode = PlaybackResizeMode.Fit
        val filmstripRequests = mutableListOf<List<Long>>()
        fun emit(value: PlaybackEngineState) {
            mutableState.value = value
        }

        override fun prepare(media: PlaybackMediaModel, startPositionMillis: Long) {
            preparedAt = startPositionMillis
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
        override suspend fun requestFilmstrip(positionsMillis: List<Long>): List<PlaybackFilmstripFrameModel> {
            filmstripRequests += positionsMillis
            delay(1L)
            return positionsMillis.map { PlaybackFilmstripFrameModel(it, null) }
        }

        override fun close() = Unit
    }

    private object FakeSurface : PlaybackVideoSurface {
        @Composable
        override fun Render(modifier: Modifier) = Unit
    }
}
