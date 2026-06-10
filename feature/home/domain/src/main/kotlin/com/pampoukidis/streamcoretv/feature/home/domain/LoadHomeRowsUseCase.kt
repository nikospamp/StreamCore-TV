package com.pampoukidis.streamcoretv.feature.home.domain

import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import javax.inject.Inject

class LoadHomeRowsUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {

    suspend operator fun invoke(profileId: String): AppResult<List<RowModel>> {
        return homeRepository.getHomeRows(profileId)
    }
}
