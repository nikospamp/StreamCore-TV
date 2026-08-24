package com.pampoukidis.streamcoretv.feature.library.domain

import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.core.model.library.LibraryEntryModel
import com.pampoukidis.streamcoretv.core.model.library.LibraryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveLibraryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playbackProgressRepository: PlaybackProgressRepository,
) {

    operator fun invoke(profileId: String): Flow<AppResult<LibraryModel>> {
        return combine(
            libraryRepository.observe(profileId),
            playbackProgressRepository.observe(profileId),
        ) { libraryResult, progressEntries ->
            when (libraryResult) {
                is AppResult.Success -> AppResult.Success(
                    createLibrary(
                        entries = libraryResult.value,
                        progressEntries = progressEntries,
                    ),
                )

                is AppResult.Failure -> libraryResult
            }
        }.catch { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }

            emit(
                AppResult.Failure(
                    AppError.Unknown(
                        source = ErrorSource(operation = ObserveOperation),
                    ),
                ),
            )
        }
    }

    private fun createLibrary(
        entries: List<LibraryEntryModel>,
        progressEntries: List<PlaybackProgressEntryModel>,
    ): LibraryModel {
        return LibraryModel(
            continueWatching = progressEntries.map(::toContinueWatchingContent),
            likedContent = entries
                .asSequence()
                .filter { entry -> entry.likedAtMillis != null }
                .sortedByDescending { entry -> entry.likedAtMillis }
                .map { entry -> entry.content.forLibraryRow(LikedRowId) }
                .toList(),
            myListContent = entries
                .asSequence()
                .filter { entry -> entry.addedToMyListAtMillis != null }
                .sortedByDescending { entry -> entry.addedToMyListAtMillis }
                .map { entry -> entry.content.forLibraryRow(MyListRowId) }
                .toList(),
        )
    }

    private fun toContinueWatchingContent(entry: PlaybackProgressEntryModel): ContentModel {
        return entry.contentSnapshot.copy(
            row = ContinueWatchingRowId,
            playbackProgress = PlaybackProgressModel(
                positionMillis = entry.positionMillis,
                durationMillis = entry.durationMillis,
            ),
        )
    }

    private fun ContentModel.forLibraryRow(rowId: String): ContentModel {
        return copy(
            row = rowId,
            playbackProgress = null,
        )
    }

    private companion object {
        const val ContinueWatchingRowId = "library:continue-watching"
        const val LikedRowId = "library:liked"
        const val MyListRowId = "library:my-list"
        const val ObserveOperation = "library.observe"
    }
}
