package com.pampoukidis.streamcoretv.feature.library.domain

import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.library.ContentLibraryStateModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveContentLibraryStateUseCase constructor(
    private val libraryRepository: LibraryRepository,
) {

    operator fun invoke(
        profileId: String,
        contentId: String,
    ): Flow<AppResult<ContentLibraryStateModel>> {
        return libraryRepository.observe(profileId).map { result ->
            when (result) {
                is AppResult.Success -> {
                    val entry = result.value.firstOrNull { value -> value.content.id == contentId }
                    AppResult.Success(
                        ContentLibraryStateModel(
                            isLiked = entry?.likedAtMillis != null,
                            isInMyList = entry?.addedToMyListAtMillis != null,
                        ),
                    )
                }

                is AppResult.Failure -> result
            }
        }
    }
}
