package com.pampoukidis.streamcoretv.core.domain

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

interface DetailsRepository {

    suspend fun getDetails(
        profileId: String,
        contentId: String,
    ): AppResult<ContentModel>

    suspend fun getRecommendations(
        profileId: String,
        contentId: String,
    ): AppResult<List<ContentModel>>
}
