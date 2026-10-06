package com.pampoukidis.streamcore.sdk.runtime.error

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

/** Typed failure for integration callbacks whose protocol signature cannot return StreamCoreResult. */
class ProviderOperationException(val error: StreamCoreError) : Exception("Provider operation failed: ${error::class.simpleName}")
