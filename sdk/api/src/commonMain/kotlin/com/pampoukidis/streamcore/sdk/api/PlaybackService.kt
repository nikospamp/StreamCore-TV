package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import kotlinx.coroutines.flow.Flow

/**
 * Playback source resolution and saved progress for the active authorized profile.
 * The application owns its player engine, rendering and navigation. Every explicit profile ID must match
 * the active authorization; flows and recorders retain the activation captured when they were created.
 */
interface PlaybackService {
    /**
     * Resolves authoritative catalogue content before applying provider policy and selecting a source.
     * A caller snapshot cannot grant access. Unsupported playback returns `StreamCoreError.Unsupported`.
     * DemoMedia capability identifies sample media, not playback of the requested catalogue title.
     */
    suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCoreResult<StreamCorePlaybackMedia>

    /**
     * Creates a stateful progress recorder for one already-resolved playback request and current activation.
     * It does not resolve media, create a player, start a timer or write progress on creation.
     * Feed player events to [PlaybackProgressRecorder.reportEvent]; the recorder timestamps updates and applies
     * periodic checkpoint cadence. [initialPositionMillis] initializes its ten-second position bucket.
     * Create a new recorder for a new playback or after profile re-entry, even when the profile ID is unchanged.
     */
    fun createProgressRecorder(request: StreamCorePlaybackRequest, initialPositionMillis: Long = 0L): PlaybackProgressRecorder

    /** Captures authorization now. Create and collect a new flow after the profile activation changes. */
    fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>>

    /** Returns the saved entry for this profile/content, or null when no allowed entry exists. */
    suspend fun getProgress(profileId: String, contentId: String): StreamCoreResult<StreamCorePlaybackProgressEntry?>

    /**
     * Applies a complete, caller-timestamped snapshot immediately, without the recorder's event/cadence logic.
     * Saves a resumable entry: positive duration, position at least 30,000 ms and position/duration below 0.95.
     * Otherwise removes existing progress for this content, including when duration is unknown/nonpositive.
     * Success means the policy was applied, not necessarily that an entry was saved.
     *
     * For ordinary player callbacks prefer [createProgressRecorder]: its recorder preserves prior progress
     * while duration is unknown and handles explicit completion separately. Do not call both paths for the
     * same player event; the recorder already performs the required update or removal.
     */
    suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit>

    /** Removes this content's saved progress within the active authorized profile. */
    suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit>
}
