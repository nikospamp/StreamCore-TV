package com.pampoukidis.streamcoretv.client.clientb.data.auth

internal interface ClientBAuthStore {
    suspend fun currentAccountId(): String? { return null }

    suspend fun setAccountId(id: String) { setLoggedIn() }

    suspend fun isLoggedIn(): Boolean

    suspend fun setLoggedIn()

    suspend fun clear()
}
