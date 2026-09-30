package com.pampoukidis.streamcore.sdk.runtime.integration.provider

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/** Account-bound backend collections. Runtime owns profile authorization and shared content-policy invocation. */
interface HomeProvider {
    suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>>
}
