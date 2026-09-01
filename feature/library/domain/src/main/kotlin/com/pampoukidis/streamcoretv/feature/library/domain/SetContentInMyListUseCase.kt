package com.pampoukidis.streamcoretv.feature.library.domain

import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

class SetContentInMyListUseCase constructor(
    private val libraryRepository: LibraryRepository,
) {

    suspend operator fun invoke(
        profileId: String,
        content: ContentModel,
        isInMyList: Boolean,
        changedAtMillis: Long = System.currentTimeMillis(),
    ): AppResult<Unit> {
        return libraryRepository.setInMyList(
            profileId = profileId,
            content = content,
            isInMyList = isInMyList,
            changedAtMillis = changedAtMillis,
        )
    }
}
