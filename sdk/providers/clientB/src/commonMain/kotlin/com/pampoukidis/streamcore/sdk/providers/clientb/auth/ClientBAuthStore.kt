package com.pampoukidis.streamcore.sdk.providers.clientb.auth

internal interface ClientBAuthStore {
    suspend fun currentAccountId(): String?

    suspend fun setAccountId(id: String)

    suspend fun clear()
}
