package com.pampoukidis.streamcore.sdk.providers.tmdb.auth

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount

internal interface TmdbAuthStore {
    suspend fun currentSessionId(): String?

    suspend fun saveSession(
        sessionId: String,
        account: StreamCoreAuthAccount?,
    )

    suspend fun clear()
}
