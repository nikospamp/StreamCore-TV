package com.pampoukidis.streamcoretv.data.tmdb.network

import com.pampoukidis.streamcoretv.core.model.error.AppResult
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes TMDB calls behind the app result boundary.
 *
 * Repositories use this to translate provider/network failures to [AppResult.Failure]
 * while preserving coroutine cancellation.
 */
internal class TmdbCallExecutor @Inject constructor(
    private val errorMapper: TmdbErrorMapper,
) {

    suspend fun <T> execute(
        operation: String,
        block: suspend () -> T,
    ): AppResult<T> {
        return try {
            AppResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            AppResult.Failure(errorMapper.map(operation = operation, throwable = throwable))
        }
    }
}
