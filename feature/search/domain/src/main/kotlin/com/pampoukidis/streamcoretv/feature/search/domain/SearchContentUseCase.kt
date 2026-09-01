package com.pampoukidis.streamcoretv.feature.search.domain

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

class SearchContentUseCase constructor(
    private val repository: SearchRepository,
) {

    suspend operator fun invoke(
        profileId: String,
        query: String,
    ): AppResult<List<ContentModel>> {
        val normalizedQuery = SearchQueryNormalizer.normalize(query)
        if (!SearchQueryNormalizer.isSearchable(normalizedQuery)) {
            return AppResult.Success(emptyList())
        }

        return repository.search(
            profileId = profileId,
            query = normalizedQuery,
        )
    }
}
