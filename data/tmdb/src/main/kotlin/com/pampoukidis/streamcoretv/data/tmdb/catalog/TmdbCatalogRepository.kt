package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbCatalogRepository @Inject constructor(
    private val catalogSource: TmdbCatalogSource,
) : HomeRepository {

    override suspend fun getHomeRows(profileId: String): AppResult<List<RowModel>> {
        if (profileId.isBlank()) {
            return catalogFailure("PROFILE_ID_REQUIRED")
        }

        return AppResult.Success(
            catalogSource.homeRows(catalogSource.contentForProfile(profileId))
                .filter { it.items.isNotEmpty() }
                .map { it.toModel() },
        )
    }

    private fun catalogFailure(backendCode: String): AppResult.Failure {
        return AppResult.Failure(
            AppError.Unknown(
                source = ErrorSource(
                    client = CLIENT,
                    operation = GET_HOME_ROWS_OPERATION,
                    backendCode = backendCode,
                ),
            ),
        )
    }

    private companion object {
        const val CLIENT = "tmdb"
        const val GET_HOME_ROWS_OPERATION = "getHomeRows"
    }
}
