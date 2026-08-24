package com.pampoukidis.streamcoretv.feature.library.domain

import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import javax.inject.Inject

class SetContentLikedUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {

    suspend operator fun invoke(
        profileId: String,
        content: ContentModel,
        isLiked: Boolean,
        changedAtMillis: Long = System.currentTimeMillis(),
    ): AppResult<Unit> {
        return libraryRepository.setLiked(
            profileId = profileId,
            content = content,
            isLiked = isLiked,
            changedAtMillis = changedAtMillis,
        )
    }
}
