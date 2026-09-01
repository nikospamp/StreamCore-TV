package com.pampoukidis.streamcoretv.feature.search.domain


class AddRecentSearchUseCase constructor(
    private val repository: RecentSearchRepository,
) {

    suspend operator fun invoke(profileId: String, query: String) {
        repository.add(profileId, SearchQueryNormalizer.normalize(query))
    }
}
