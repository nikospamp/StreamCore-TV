package com.pampoukidis.streamcoretv.feature.search.domain

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

interface SearchRepository {

    suspend fun search(
        profileId: String,
        query: String,
    ): AppResult<List<ContentModel>>

    suspend fun loadTrending(profileId: String): AppResult<List<ContentModel>>
}
