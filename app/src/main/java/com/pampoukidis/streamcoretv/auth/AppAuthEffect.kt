package com.pampoukidis.streamcoretv.auth

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

sealed interface AppAuthEffect {
    data class ShowError(
        val error: StreamCoreError,
    ) : AppAuthEffect
}
