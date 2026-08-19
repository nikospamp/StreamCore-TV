package com.pampoukidis.streamcoretv.feature.search.domain

import javax.inject.Inject

class ClearRecentSearchesUseCase @Inject constructor(
    private val repository: RecentSearchRepository,
) {

    suspend operator fun invoke(profileId: String) {
        repository.clear(profileId)
    }
}
