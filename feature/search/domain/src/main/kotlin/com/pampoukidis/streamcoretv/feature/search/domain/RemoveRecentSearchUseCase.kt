package com.pampoukidis.streamcoretv.feature.search.domain

import javax.inject.Inject

class RemoveRecentSearchUseCase @Inject constructor(
    private val repository: RecentSearchRepository,
) {

    suspend operator fun invoke(profileId: String, query: String) {
        repository.remove(profileId, SearchQueryNormalizer.normalize(query))
    }
}
