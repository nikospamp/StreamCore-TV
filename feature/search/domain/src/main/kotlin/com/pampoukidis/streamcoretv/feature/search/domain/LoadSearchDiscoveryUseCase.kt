package com.pampoukidis.streamcoretv.feature.search.domain

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

class LoadSearchDiscoveryUseCase constructor(
    private val repository: SearchRepository,
) {

    suspend operator fun invoke(profileId: String): AppResult<List<ContentModel>> {
        return repository.loadTrending(profileId)
    }
}
