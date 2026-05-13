package com.pampoukidis.streamcoretv.common.domain

interface AuthenticateRepository {

    suspend fun loginUser(
        email: String,
        password: String
    )

    suspend fun loginUserWithQR(
        qrCode: String
    )

    suspend fun logoutUser()

    suspend fun forgotPassword(
        email: String,
        otp: String? = null
    )
}