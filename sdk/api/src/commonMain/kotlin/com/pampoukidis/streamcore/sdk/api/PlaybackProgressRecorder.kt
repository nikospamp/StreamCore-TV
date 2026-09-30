package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEvent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/**
 * Receives player events for one resolved request and captured profile activation.
 * Created by [PlaybackService.createProgressRecorder]; it does not play media or poll a player.
 * The recorder owns checkpoint cadence and timestamps, while the SDK owns authorization and resume policy.
 */
interface PlaybackProgressRecorder {
    /**
     * Processes one player event; the application supplies the current position and duration in milliseconds.
     *
     * - `Periodic`: with known positive duration, attempts an update only after entering a higher ten-second
     *   position bucket than previously attempted. Repeated samples in the same/earlier bucket are ignored.
     * - `Checkpoint`: with known positive duration, attempts an update immediately, bypassing the periodic
     *   gate. Use for pause, seek or exit; this event does not reset the periodic bucket.
     * - `Completed`: removes saved progress, even when duration is unknown or position is below the end.
     *
     * Nonpositive duration for Periodic/Checkpoint is ignored and preserves existing progress. Attempted
     * updates use the current timestamp and apply the same resume policy as [PlaybackService.updateProgress]:
     * positions before 30 seconds or at/above 95% completion remove the entry instead of saving it.
     * Success can therefore mean saved, removed or intentionally ignored; it is not a write receipt.
     *
     * A periodic bucket advances when an attempt starts, including a failed attempt. There is no automatic
     * retry; a Checkpoint can explicitly retry without waiting for a higher bucket. Failures are returned as
     * [StreamCoreResult.Failure] and coroutine cancellation propagates. A persistence failure must not trap player exit.
     * Do not also call [PlaybackService.updateProgress] for the same event.
     */
    suspend fun reportEvent(
        event: StreamCorePlaybackProgressEvent,
        positionMillis: Long,
        durationMillis: Long,
    ): StreamCoreResult<Unit>
}
