package com.pampoukidis.streamcoretv.feature.search.domain

import kotlinx.coroutines.flow.Flow

interface RecentSearchRepository {

    fun observe(profileId: String): Flow<List<String>>

    suspend fun add(profileId: String, query: String)

    suspend fun remove(profileId: String, query: String)

    suspend fun clear(profileId: String)
}
