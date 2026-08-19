package com.pampoukidis.streamcoretv.feature.search.domain

import javax.inject.Inject

class AddRecentSearchUseCase @Inject constructor(
    private val repository: RecentSearchRepository,
) {

    suspend operator fun invoke(profileId: String, query: String) {
        repository.add(profileId, SearchQueryNormalizer.normalize(query))
    }
}
