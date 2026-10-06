package com.pampoukidis.streamcore.sdk.runtime.library

import com.pampoukidis.streamcore.sdk.api.LibraryService
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreContextFailureReason
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreContentLibraryState
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibrary
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibraryEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgress
import com.pampoukidis.streamcore.sdk.runtime.playback.RuntimePlaybackService
import com.pampoukidis.streamcore.sdk.runtime.session.AuthorizedProfile
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.session.contextFailure
import com.pampoukidis.streamcore.sdk.runtime.session.failure
import com.pampoukidis.streamcore.sdk.runtime.session.invalid
import com.pampoukidis.streamcore.sdk.runtime.session.unsupported
import com.pampoukidis.streamcore.sdk.runtime.storage.library.LibraryStore
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Owns library reads and membership writes. Mutations capture authorization, timestamp the change,
 * enforce profile/content rules and write the real store here; they never call back into the client.
 * Library and progress observations must use the same captured authorization to prevent mixed accounts.
 */
internal class RuntimeLibraryService(
    private val runtimeSession: RuntimeSession,
    private val capabilities: StreamCoreCapabilities,
    private val library: LibraryStore,
    private val playback: RuntimePlaybackService,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : LibraryService {
    override fun observe(profileId: String): Flow<StreamCoreResult<StreamCoreLibrary>> {
        val authorization = runtimeSession.currentAuthorization
        val libraryEntries = observeAllowedEntries(profileId, authorization)
        val playbackEntries = playback.observeProgress(profileId, authorization)

        return combine(libraryEntries, playbackEntries) { libraryResult, progressResult ->
            when {
                libraryResult is StreamCoreResult.Failure -> {
                    libraryResult
                }
                progressResult is StreamCoreResult.Failure -> {
                    progressResult
                }
                libraryResult is StreamCoreResult.Success && progressResult is StreamCoreResult.Success -> {
                    val continueWatching = progressResult.value.map { entry ->
                        entry.contentSnapshot.copy(
                            row = "library:continue-watching",
                            playbackProgress = StreamCorePlaybackProgress(
                                positionMillis = entry.positionMillis,
                                durationMillis = entry.durationMillis,
                            ),
                        )
                    }
                    val likedContent = libraryResult.value
                        .filter { entry -> entry.likedAtMillis != null }
                        .sortedByDescending { entry -> entry.likedAtMillis }
                        .map { entry ->
                            entry.content.copy(row = "library:liked", playbackProgress = null)
                        }
                    val myListContent = libraryResult.value
                        .filter { entry -> entry.addedToMyListAtMillis != null }
                        .sortedByDescending { entry -> entry.addedToMyListAtMillis }
                        .map { entry ->
                            entry.content.copy(row = "library:my-list", playbackProgress = null)
                        }

                    StreamCoreResult.Success(
                        StreamCoreLibrary(
                            continueWatching = continueWatching,
                            likedContent = likedContent,
                            myListContent = myListContent,
                        ),
                    )
                }
                else -> {
                    error("Unreachable result state")
                }
            }
        }.map { result ->
            if (authorization == null) result else runtimeSession.checkProfileResult(authorization, result)
        }
    }

    override fun observeContentState(
        profileId: String,
        contentId: String,
    ): Flow<StreamCoreResult<StreamCoreContentLibraryState>> {
        return observeAllowedEntries(profileId, runtimeSession.currentAuthorization).map { result ->
            when (result) {
                is StreamCoreResult.Failure -> {
                    result
                }
                is StreamCoreResult.Success -> {
                    val entry = result.value.find { it.content.id == contentId }
                    val contentState = StreamCoreContentLibraryState(
                        isLiked = entry?.likedAtMillis != null,
                        isInMyList = entry?.addedToMyListAtMillis != null,
                    )
                    StreamCoreResult.Success(contentState)
                }
            }
        }
    }

    override suspend fun setLiked(
        profileId: String,
        content: StreamCoreContent,
        isLiked: Boolean,
    ): StreamCoreResult<Unit> {
        val authorization = runtimeSession.currentAuthorization
        val changedAtMillis = nowMillis()
        return changeMembership(profileId, content, changedAtMillis, authorization) { storageKey ->
            library.setLiked(storageKey, content, isLiked, changedAtMillis)
        }
    }

    override suspend fun setInMyList(
        profileId: String,
        content: StreamCoreContent,
        isInMyList: Boolean,
    ): StreamCoreResult<Unit> {
        val authorization = runtimeSession.currentAuthorization
        val changedAtMillis = nowMillis()
        return changeMembership(profileId, content, changedAtMillis, authorization) { storageKey ->
            library.setInMyList(storageKey, content, isInMyList, changedAtMillis)
        }
    }

    private fun observeAllowedEntries(
        profileId: String,
        authorization: AuthorizedProfile?,
    ): Flow<StreamCoreResult<List<StreamCoreLibraryEntry>>> {
        return runtimeSession.observeStoredProfileData(profileId, authorization, capabilities.localLibrary, "library") { captured, profile ->
            library.observe(runtimeSession.storageKey(captured)).map { result ->
                when (result) {
                    is StreamCoreResult.Failure -> result
                    is StreamCoreResult.Success -> {
                        val allowedEntries = result.value.filter {
                            captured.providers.contentPolicy.isContentAllowed(profile, it.content)
                        }
                        StreamCoreResult.Success(allowedEntries)
                    }
                }
            }
        }
    }

    private suspend fun changeMembership(
        profileId: String,
        content: StreamCoreContent,
        changedAtMillis: Long,
        authorization: AuthorizedProfile?,
        write: suspend (storageKey: String) -> StreamCoreResult<Unit>,
    ): StreamCoreResult<Unit> {
        if (!capabilities.localLibrary) return unsupported("library")
        if (content.id.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
        if (changedAtMillis < 0L) return invalid(StreamCoreValidationField.ChangedAtMillis, StreamCoreValidationReason.OutOfRange)
        return runtimeSession.withAuthorizedProfile(profileId, authorization) { captured, profile ->
            if (!captured.providers.contentPolicy.isContentAllowed(profile, content)) {
                failure(StreamCoreError.Unauthorized())
            } else if (!runtimeSession.isCurrent(captured)) {
                contextFailure(StreamCoreContextFailureReason.StaleActivation)
            } else {
                write(runtimeSession.storageKey(captured))
            }
        }
    }
}
