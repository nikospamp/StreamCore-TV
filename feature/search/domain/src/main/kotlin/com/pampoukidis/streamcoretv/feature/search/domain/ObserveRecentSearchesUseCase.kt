package com.pampoukidis.streamcoretv.feature.search.domain

import kotlinx.coroutines.flow.Flow

class ObserveRecentSearchesUseCase constructor(
    private val repository: RecentSearchRepository,
) {

    operator fun invoke(profileId: String): Flow<List<String>> {
        return repository.observe(profileId)
    }
}
