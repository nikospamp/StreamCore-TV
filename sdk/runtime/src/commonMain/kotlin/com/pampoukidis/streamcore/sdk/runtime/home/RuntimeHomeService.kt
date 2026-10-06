package com.pampoukidis.streamcore.sdk.runtime.home

import com.pampoukidis.streamcore.sdk.api.HomeService
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import kotlinx.coroutines.flow.map

/** Shared home behavior; backend work stays in the matching provider. */
internal class RuntimeHomeService(
    private val runtimeSession: RuntimeSession,
) : HomeService {
    override suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>> {
        return runtimeSession.withAuthorizedProfile(profileId) { authorization, profile ->
            val services = authorization.providers
            when (val result = services.home.getCollections(profileId)) {
                is StreamCoreResult.Failure -> result
                is StreamCoreResult.Success -> StreamCoreResult.Success(result.value.map { row -> row.copy(content = row.content.filter { services.contentPolicy.isContentAllowed(profile, it) }) })
            }
        }
    }
}
