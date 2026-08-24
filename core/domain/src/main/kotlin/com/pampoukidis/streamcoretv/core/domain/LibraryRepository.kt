package com.pampoukidis.streamcoretv.core.domain

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.library.LibraryEntryModel
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {

    fun observe(profileId: String): Flow<AppResult<List<LibraryEntryModel>>>

    suspend fun setLiked(
        profileId: String,
        content: ContentModel,
        isLiked: Boolean,
        changedAtMillis: Long,
    ): AppResult<Unit>

    suspend fun setInMyList(
        profileId: String,
        content: ContentModel,
        isInMyList: Boolean,
        changedAtMillis: Long,
    ): AppResult<Unit>
}
