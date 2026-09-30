package com.pampoukidis.streamcore.sdk.runtime.storage.search

import kotlinx.coroutines.flow.Flow

interface SearchHistoryStore {

    fun observe(profileId: String): Flow<List<String>>

    suspend fun add(profileId: String, query: String)

    suspend fun remove(profileId: String, query: String)

    suspend fun clear(profileId: String)
}
