package com.pampoukidis.streamcoretv.client.clientb.data.auth

internal interface ClientBAuthStore {
    suspend fun isLoggedIn(): Boolean

    suspend fun setLoggedIn()

    suspend fun clear()
}
