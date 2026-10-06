package com.pampoukidis.streamcore.sdk.runtime.playback

import com.pampoukidis.streamcore.sdk.api.PlaybackProgressRecorder
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.validation.PlaybackProgressPolicy
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreContextFailureReason
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEvent
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackSupport
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.session.AuthorizedProfile
import com.pampoukidis.streamcore.sdk.runtime.session.contextFailure
import com.pampoukidis.streamcore.sdk.runtime.session.failure
import com.pampoukidis.streamcore.sdk.runtime.session.invalid
import com.pampoukidis.streamcore.sdk.runtime.session.unsupported
import com.pampoukidis.streamcore.sdk.runtime.storage.playback.PlaybackProgressStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * Owns source resolution, saved progress and playback-event handling in one place.
 * Each private recorder captures its own authorization and cadence; applications own the player engine.
 */
internal class RuntimePlaybackService(
    private val runtimeSession: RuntimeSession,
    private val capabilities: StreamCoreCapabilities,
    private val store: PlaybackProgressStore,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : PlaybackService {
    override suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCoreResult<StreamCorePlaybackMedia> {
        if (capabilities.playback == StreamCorePlaybackSupport.Unsupported) return unsupported("playback.sources")
        if (request.contentId.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
        if (request.contentSnapshot.id != request.contentId) return invalid(StreamCoreValidationField.ContentSnapshot, StreamCoreValidationReason.Mismatch)
        return runtimeSession.withAuthorizedProfile(request.profileId) { authorization, profile ->
            val services = authorization.providers
            when (val content = services.details.getDetails(profile.id, request.contentId)) {
                is StreamCoreResult.Failure -> content
                is StreamCoreResult.Success -> {
                    if (!services.contentPolicy.isContentAllowed(profile, content.value)) failure(StreamCoreError.Unauthorized())
                    else if (!runtimeSession.isCurrent(authorization)) contextFailure(StreamCoreContextFailureReason.StaleActivation)
                    else StreamCoreResult.Success(services.playback.resolveSource(request.copy(contentSnapshot = content.value)))
                }
            }
        }
    }

    override fun createProgressRecorder(request: StreamCorePlaybackRequest, initialPositionMillis: Long): PlaybackProgressRecorder {
        val authorization = runtimeSession.currentAuthorization
        return ProgressRecorder(request, authorization, initialPositionMillis)
    }

    override fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
        return observeProgress(profileId, runtimeSession.currentAuthorization)
    }

    /** The library supplies its captured authorization so both combined sources belong to the same grant. */
    fun observeProgress(
        profileId: String,
        captured: AuthorizedProfile?,
    ): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
        return runtimeSession.observeStoredProfileData(profileId, captured, capabilities.playbackProgress, "playback.progress") { authorization, profile ->
            store.observe(runtimeSession.storageKey(authorization)).map { entries ->
                val allowedEntries = entries.filter {
                    authorization.providers.contentPolicy.isContentAllowed(profile, it.contentSnapshot)
                }
                val profileEntries = allowedEntries.map { it.copy(profileId = profileId) }
                StreamCoreResult.Success(profileEntries)
            }
        }
    }

    override suspend fun getProgress(profileId: String, contentId: String): StreamCoreResult<StreamCorePlaybackProgressEntry?> {
        val captured = runtimeSession.currentAuthorization
        if (!capabilities.playbackProgress) return unsupported("playback.progress")
        return runtimeSession.withAuthorizedProfile(profileId, captured) { authorization, profile ->
            val entry = store.get(runtimeSession.storageKey(authorization), contentId)
            StreamCoreResult.Success(entry?.takeIf { authorization.providers.contentPolicy.isContentAllowed(profile, it.contentSnapshot) }?.copy(profileId = profileId))
        }
    }

    override suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit> {
        return updateProgress(entry, runtimeSession.currentAuthorization)
    }

    override suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit> {
        return removeProgress(profileId, contentId, runtimeSession.currentAuthorization)
    }

    private suspend fun updateProgress(
        entry: StreamCorePlaybackProgressEntry,
        captured: AuthorizedProfile?,
    ): StreamCoreResult<Unit> {
        if (!capabilities.playbackProgress) return unsupported("playback.progress")
        if (entry.contentId.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
        if (entry.contentId != entry.contentSnapshot.id) return invalid(StreamCoreValidationField.ContentSnapshot, StreamCoreValidationReason.Mismatch)
        if (entry.updatedAtMillis < 0L) return invalid(StreamCoreValidationField.ChangedAtMillis, StreamCoreValidationReason.OutOfRange)
        return runtimeSession.withAuthorizedProfile(entry.profileId, captured) { authorization, profile ->
            if (!authorization.providers.contentPolicy.isContentAllowed(profile, entry.contentSnapshot)) {
                return@withAuthorizedProfile failure(StreamCoreError.Unauthorized())
            }
            if (!runtimeSession.isCurrent(authorization)) {
                return@withAuthorizedProfile contextFailure(StreamCoreContextFailureReason.StaleActivation)
            }
            if (PlaybackProgressPolicy.isResumable(entry.positionMillis, entry.durationMillis)) {
                store.upsert(entry.copy(profileId = runtimeSession.storageKey(authorization)))
            } else {
                store.remove(runtimeSession.storageKey(authorization), entry.contentId)
            }
            StreamCoreResult.Success(Unit)
        }
    }

    private suspend fun removeProgress(
        profileId: String,
        contentId: String,
        captured: AuthorizedProfile?,
    ): StreamCoreResult<Unit> {
        if (!capabilities.playbackProgress) return unsupported("playback.progress")
        return runtimeSession.withAuthorizedProfile(profileId, captured) { authorization, _ ->
            store.remove(runtimeSession.storageKey(authorization), contentId)
            StreamCoreResult.Success(Unit)
        }
    }

    /** State belongs to one playback, never to the shared service or whichever profile becomes active later. */
    private inner class ProgressRecorder(
        private val request: StreamCorePlaybackRequest,
        private val captured: AuthorizedProfile?,
        initialPositionMillis: Long,
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
                        return@withLock removeProgress(request.profileId, request.contentId, captured)
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
                    updateProgress(progressEntry, captured)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    StreamCoreResult.Failure(StreamCoreError.Storage())
                }
            }
        }
    }

    private companion object {
        const val SaveIntervalMillis = 10_000L
    }
}
