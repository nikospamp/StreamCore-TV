package com.pampoukidis.streamcore.sdk.runtime.details

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/** Authoritative backend content and recommendations, also used to validate direct playback requests. */
interface DetailsProvider {
    suspend fun getDetails(profileId: String, contentId: String): StreamCoreResult<StreamCoreContent>
    suspend fun getRecommendations(profileId: String, contentId: String): StreamCoreResult<List<StreamCoreContent>>
}
