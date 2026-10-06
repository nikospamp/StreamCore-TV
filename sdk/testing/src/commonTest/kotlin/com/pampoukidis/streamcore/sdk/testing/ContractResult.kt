package com.pampoukidis.streamcore.sdk.testing

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult

/** Unwraps a successful SDK operation or fails the calling provider test with its error. */
internal fun <T> StreamCoreResult<T>.contractValue(): T {
    return when (this) {
        is StreamCoreResult.Success -> value
        is StreamCoreResult.Failure -> error("Provider contract operation failed: $error")
    }
}
