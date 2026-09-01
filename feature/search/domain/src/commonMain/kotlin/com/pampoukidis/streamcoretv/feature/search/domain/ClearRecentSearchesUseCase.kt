package com.pampoukidis.streamcoretv.feature.search.domain


class ClearRecentSearchesUseCase constructor(
    private val repository: RecentSearchRepository,
) {

    suspend operator fun invoke(profileId: String) {
        repository.clear(profileId)
    }
}
