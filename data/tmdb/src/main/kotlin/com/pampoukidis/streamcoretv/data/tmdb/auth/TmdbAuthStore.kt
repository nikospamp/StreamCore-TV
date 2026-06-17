package com.pampoukidis.streamcoretv.data.tmdb.auth

import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import kotlinx.coroutines.flow.Flow

internal interface TmdbAuthStore {
    val authState: Flow<AuthStateModel>

    suspend fun currentSessionId(): String?

    suspend fun saveSession(
        sessionId: String,
        account: AuthAccountModel?,
    )

    suspend fun clear()
}