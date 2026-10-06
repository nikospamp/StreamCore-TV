package com.pampoukidis.streamcore.sdk.providers.clientb.home

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.providers.clientb.catalog.ClientBCatalogSource
import com.pampoukidis.streamcore.sdk.runtime.home.HomeProvider

internal class ClientBHomeRepository constructor(
    private val catalogSource: ClientBCatalogSource,
) : HomeProvider {

    override suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>> {
        if (profileId.isBlank()) {
            return catalogFailure("PROFILE_ID_REQUIRED")
        }

        return StreamCoreResult.Success(
            catalogSource.homeLanes(catalogSource.contentForProfile(profileId))
                .filter { it.assets.isNotEmpty() }
                .map { it.toModel() },
        )
    }

    private fun catalogFailure(backendCode: String): StreamCoreResult.Failure {
        return StreamCoreResult.Failure(
            StreamCoreError.Unknown(
                source = StreamCoreErrorSource(
                    client = CLIENT,
                    operation = GET_HOME_ROWS_OPERATION,
                    backendCode = backendCode,
                ),
            ),
        )
    }

    private companion object {
        const val CLIENT = "clientB"
        const val GET_HOME_ROWS_OPERATION = "getHomeRows"
    }
}
