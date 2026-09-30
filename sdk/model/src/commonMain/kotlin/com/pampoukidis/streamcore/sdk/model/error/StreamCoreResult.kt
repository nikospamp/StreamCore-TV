package com.pampoukidis.streamcore.sdk.model.error

/**
 * Expected consumer-operation outcome. Suspend calls still propagate coroutine cancellation.
 * This type does not make configuration construction or host callbacks exception-free. Observe context for state changes.
 */
sealed interface StreamCoreResult<out T> {
    data class Success<T>(val value: T) : StreamCoreResult<T>
    data class Failure(val error: StreamCoreError) : StreamCoreResult<Nothing>
}
