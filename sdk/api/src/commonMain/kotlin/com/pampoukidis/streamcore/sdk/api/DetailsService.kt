package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/** Detail/recommendation access requires the supplied profile ID to match the current authorized activation. */
interface DetailsService {
    /** Resolves authoritative catalogue content and applies provider restrictions to direct requests as well. */
    suspend fun getDetails(profileId: String, contentId: String): StreamCoreResult<StreamCoreContent>
    /** Returns provider-ordered recommendations filtered by the current profile's content policy. */
    suspend fun getRecommendations(profileId: String, contentId: String): StreamCoreResult<List<StreamCoreContent>>
}
