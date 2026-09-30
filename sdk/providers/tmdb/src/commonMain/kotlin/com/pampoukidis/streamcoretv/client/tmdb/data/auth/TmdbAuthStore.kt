package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount

internal interface TmdbAuthStore {
    suspend fun currentSessionId(): String?

    suspend fun legacyAccountId(): String? { return null }

    suspend fun saveSession(
        sessionId: String,
        account: StreamCoreAuthAccount?,
    )

    suspend fun clear()
}
