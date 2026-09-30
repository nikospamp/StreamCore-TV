package com.pampoukidis.streamcore.sdk.runtime

import com.pampoukidis.streamcore.sdk.api.PlaybackProgressRecorder
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEvent
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

internal class RuntimePlaybackProgressRecorder(
    private val request: StreamCorePlaybackRequest,
    private val progress: PlaybackProgressOperations,
    initialPositionMillis: Long,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : PlaybackProgressRecorder {
    private val mutex = Mutex()
    private var lastAttemptedPeriodicBucket = initialPositionMillis / SaveIntervalMillis

    override suspend fun reportEvent(
        event: StreamCorePlaybackProgressEvent,
        positionMillis: Long,
        durationMillis: Long,
    ): StreamCoreResult<Unit> {
        return mutex.withLock {
            try {
                if (event == StreamCorePlaybackProgressEvent.Completed) {
                    return@withLock progress.removeProgress(request.profileId, request.contentId)
                }

                if (durationMillis <= 0L) {
                    return@withLock StreamCoreResult.Success(Unit)
                }

                if (event == StreamCorePlaybackProgressEvent.Periodic) {
                    val positionBucket = positionMillis / SaveIntervalMillis
                    if (positionBucket <= lastAttemptedPeriodicBucket) {
                        return@withLock StreamCoreResult.Success(Unit)
                    }

                    // Advance before persistence: a failed periodic attempt still consumes this bucket.
                    lastAttemptedPeriodicBucket = positionBucket
                }

                val progressEntry = StreamCorePlaybackProgressEntry(
                    profileId = request.profileId,
                    contentId = request.contentId,
                    contentSnapshot = request.contentSnapshot,
                    positionMillis = positionMillis,
                    durationMillis = durationMillis,
                    updatedAtMillis = nowMillis(),
                )
                progress.updateProgress(progressEntry)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                StreamCoreResult.Failure(StreamCoreError.Storage())
            }
        }
    }

    private companion object {
        const val SaveIntervalMillis = 10_000L
    }
}
