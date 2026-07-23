package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel

internal interface TmdbAuthStore {
    suspend fun currentSessionId(): String?

    suspend fun saveSession(
        sessionId: String,
        account: AuthAccountModel?,
    )

    suspend fun clear()
}