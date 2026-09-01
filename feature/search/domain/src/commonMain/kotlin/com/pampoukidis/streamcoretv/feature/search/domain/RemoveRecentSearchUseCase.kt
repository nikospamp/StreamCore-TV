package com.pampoukidis.streamcoretv.feature.search.domain


class RemoveRecentSearchUseCase constructor(
    private val repository: RecentSearchRepository,
) {

    suspend operator fun invoke(profileId: String, query: String) {
        repository.remove(profileId, SearchQueryNormalizer.normalize(query))
    }
}
