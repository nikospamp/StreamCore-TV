package com.pampoukidis.streamcore.sdk.runtime

import com.pampoukidis.streamcore.sdk.api.LibraryService
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreContentLibraryState
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibrary
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgress
import com.pampoukidis.streamcore.sdk.runtime.storage.library.LibraryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** Builds library rows and supplies membership timestamps to the runtime's activation-bound store. */
internal class RuntimeLibraryService(
    private val library: LibraryStore,
    private val progress: PlaybackProgressOperations,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : LibraryService {
    override fun observe(profileId: String): Flow<StreamCoreResult<StreamCoreLibrary>> {
        val libraryEntries = library.observe(profileId)
        val playbackEntries = progress.observeProgress(profileId)

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
        }
    }

    override fun observeContentState(
        profileId: String,
        contentId: String,
    ): Flow<StreamCoreResult<StreamCoreContentLibraryState>> {
        return library.observe(profileId).map { result ->
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
        val changedAtMillis = nowMillis()
        return library.setLiked(profileId, content, isLiked, changedAtMillis)
    }

    override suspend fun setInMyList(
        profileId: String,
        content: StreamCoreContent,
        isInMyList: Boolean,
    ): StreamCoreResult<Unit> {
        val changedAtMillis = nowMillis()
        return library.setInMyList(profileId, content, isInMyList, changedAtMillis)
    }
}
