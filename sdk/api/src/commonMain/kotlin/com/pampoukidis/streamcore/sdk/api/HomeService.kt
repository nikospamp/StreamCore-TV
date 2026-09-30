package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/** Catalogue collections for the currently authorized profile. Every supplied profile ID must match that activation. */
interface HomeService {
    /** Returns ordered semantic collections after provider content restrictions; applications choose their layout. */
    suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>>
}
