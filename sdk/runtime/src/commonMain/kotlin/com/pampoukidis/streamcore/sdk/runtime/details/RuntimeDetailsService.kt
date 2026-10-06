package com.pampoukidis.streamcore.sdk.runtime.details

import com.pampoukidis.streamcore.sdk.api.DetailsService
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.session.failure
import com.pampoukidis.streamcore.sdk.runtime.session.invalid

/** Shared details behavior; backend work stays in the matching provider. */
internal class RuntimeDetailsService(
    private val runtimeSession: RuntimeSession,
) : DetailsService {
    override suspend fun getDetails(profileId: String, contentId: String): StreamCoreResult<StreamCoreContent> {
        if (contentId.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
        return runtimeSession.withAuthorizedProfile(profileId) { authorization, profile ->
            val services = authorization.providers
            when (val result = services.details.getDetails(profileId, contentId)) {
                is StreamCoreResult.Failure -> result
                is StreamCoreResult.Success -> if (services.contentPolicy.isContentAllowed(profile, result.value)) result else failure(StreamCoreError.Unauthorized())
            }
        }
    }
    override suspend fun getRecommendations(profileId: String, contentId: String): StreamCoreResult<List<StreamCoreContent>> {
        if (contentId.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
        return runtimeSession.withAuthorizedProfile(profileId) { authorization, profile -> runtimeSession.filterAllowedContents(authorization.providers.details.getRecommendations(profileId, contentId), authorization, profile) }
    }
}
