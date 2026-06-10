package com.pampoukidis.streamcoretv.core.domain

import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

interface HomeRepository {

    suspend fun getHomeRows(profileId: String): AppResult<List<RowModel>>
}