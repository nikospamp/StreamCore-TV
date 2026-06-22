package com.pampoukidis.streamcoretv.data.tmdb.auth

import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel

internal interface TmdbAuthStore {
    suspend fun currentSessionId(): String?

    suspend fun saveSession(
        sessionId: String,
        account: AuthAccountModel?,
    )

    suspend fun clear()
}