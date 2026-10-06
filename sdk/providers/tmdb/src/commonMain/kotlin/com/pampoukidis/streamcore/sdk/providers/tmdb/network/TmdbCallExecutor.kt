package com.pampoukidis.streamcore.sdk.providers.tmdb.network

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executes TMDB calls behind the app result boundary.
 *
 * Repositories use this to translate provider/network failures to [StreamCoreResult.Failure]
 * while preserving coroutine cancellation.
 */
internal class TmdbCallExecutor constructor(
    private val errorMapper: TmdbErrorMapper,
) {

    suspend fun <T> execute(
        operation: String,
        block: suspend () -> T,
    ): StreamCoreResult<T> {
        return try {
            StreamCoreResult.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            StreamCoreResult.Failure(errorMapper.map(operation = operation, throwable = throwable))
        }
    }
}
