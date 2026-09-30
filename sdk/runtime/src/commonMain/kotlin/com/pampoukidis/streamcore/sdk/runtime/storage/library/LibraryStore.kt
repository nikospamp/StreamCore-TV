package com.pampoukidis.streamcore.sdk.runtime.storage.library

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibraryEntry
import kotlinx.coroutines.flow.Flow

interface LibraryStore {

    fun observe(profileId: String): Flow<StreamCoreResult<List<StreamCoreLibraryEntry>>>

    suspend fun setLiked(
        profileId: String,
        content: StreamCoreContent,
        isLiked: Boolean,
        changedAtMillis: Long,
    ): StreamCoreResult<Unit>

    suspend fun setInMyList(
        profileId: String,
        content: StreamCoreContent,
        isInMyList: Boolean,
        changedAtMillis: Long,
    ): StreamCoreResult<Unit>
}
