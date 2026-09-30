package com.pampoukidis.streamcore.sdk.runtime

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RuntimePlaybackProgressRecorderTest {
    @Test
    fun failedPeriodicAttemptKeepsBucketUntilForwardAdvanceAndCheckpointStillWrites(): TestResult {
        return runTest {
            val service = RecordingProgressService()
            val recorder = RuntimePlaybackProgressRecorder(request(), service, 0L)
            service.failure = IllegalStateException("storage unavailable")
            assertIs<StreamCoreResult.Failure>(recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 40_000L, 100_000L))

            service.failure = null
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 49_999L, 100_000L)
            assertEquals(emptyList(), service.entries)
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 35_000L, 100_000L)
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 40_001L, 100_000L)
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 50_000L, 100_000L)
            assertEquals(listOf(35_000L, 50_000L), service.entries.map { it.positionMillis })
        }
    }

    @Test
    fun periodicSamplesKeepTheEstablishedTenSecondCadenceAndResumeBucket() = runTest {
        val service = RecordingProgressService()
        val recorder = RuntimePlaybackProgressRecorder(request(), service, 45_000L, nowMillis = { 123L })

        listOf(45_000L, 49_999L, 50_000L, 50_001L, 59_999L, 60_000L).forEach { position ->
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, position, 100_000L)
        }

        assertEquals(listOf(50_000L, 60_000L), service.entries.map { it.positionMillis })
        assertEquals(listOf(123L, 123L), service.entries.map { it.updatedAtMillis })
    }

    @Test
    fun explicitCheckpointsAreNotSuppressedByCadenceAndUnknownDurationPreservesData() = runTest {
        val service = RecordingProgressService()
        val recorder = RuntimePlaybackProgressRecorder(request(), service, 40_000L)

        recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_001L, 100_000L)
        recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_002L, 100_000L)
        recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 0L, 0L)
        recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 60_000L, 0L)

        assertEquals(listOf(40_001L, 40_002L), service.entries.map { it.positionMillis })
        assertEquals(0, service.removals)
    }

    @Test
    fun completionRemovesProgressAndStorageFailureDoesNotThrowOrBlockLaterEvents() = runTest {
        val service = RecordingProgressService()
        val recorder = RuntimePlaybackProgressRecorder(request(), service, 0L)
        service.failure = IllegalStateException("storage unavailable")

        assertIs<StreamCoreError.Storage>(assertIs<StreamCoreResult.Failure>(
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_000L, 100_000L),
        ).error)
        assertIs<StreamCoreResult.Failure>(recorder.reportEvent(StreamCorePlaybackProgressEvent.Completed, 100_000L, 100_000L))

        service.failure = null
        assertEquals(StreamCoreResult.Success(Unit), recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 50_000L, 100_000L))
        assertEquals(StreamCoreResult.Success(Unit), recorder.reportEvent(StreamCorePlaybackProgressEvent.Completed, 100_000L, 100_000L))
        assertEquals(2, service.removals)
    }

    @Test
    fun cancelledPersistenceRemainsCancellation() = runTest {
        val service = RecordingProgressService()
        val cancellation = CancellationException("cancelled checkpoint")
        service.failure = cancellation
        val recorder = RuntimePlaybackProgressRecorder(request(), service, 0L)

        assertEquals(cancellation, assertFailsWith<CancellationException> {
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_000L, 100_000L)
        })
    }

    private class RecordingProgressService : PlaybackProgressOperations {
        val entries = mutableListOf<StreamCorePlaybackProgressEntry>()
        var removals = 0
        var failure: Exception? = null
        override fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
            return flowOf(StreamCoreResult.Success(entries))
        }
        override suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit> {
            failure?.let { throw it }
            entries += entry
            return StreamCoreResult.Success(Unit)
        }
        override suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit> {
            removals += 1
            failure?.let { throw it }
            return StreamCoreResult.Success(Unit)
        }
    }
}

private fun request(): StreamCorePlaybackRequest {
    val content = StreamCoreContent("film", "Film", "", 0, "", 0, "", null, emptyList(), 0L, emptyList())
    return StreamCorePlaybackRequest("profile", content.id, content)
}
