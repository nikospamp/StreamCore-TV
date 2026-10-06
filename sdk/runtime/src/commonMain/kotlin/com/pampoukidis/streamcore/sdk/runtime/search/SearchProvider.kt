package com.pampoukidis.streamcore.sdk.runtime.search

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/** Backend search and discovery. Runtime owns query normalization, interaction rules and local search history. */
interface SearchProvider {
    suspend fun search(profileId: String, query: String): StreamCoreResult<List<StreamCoreContent>>
    suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>>
}
