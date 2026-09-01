package com.pampoukidis.streamcoretv.feature.library.domain

import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

class SetContentLikedUseCase constructor(
    private val libraryRepository: LibraryRepository,
    private val clock: LibraryClock = SystemLibraryClock,
) {

    suspend operator fun invoke(
        profileId: String,
        content: ContentModel,
        isLiked: Boolean,
        changedAtMillis: Long = clock.nowEpochMillis(),
    ): AppResult<Unit> {
        return libraryRepository.setLiked(
            profileId = profileId,
            content = content,
            isLiked = isLiked,
            changedAtMillis = changedAtMillis,
        )
    }
}
