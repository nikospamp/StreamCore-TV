package com.pampoukidis.streamcoretv.data.clientb.auth

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import javax.inject.Inject

class ClientBAuthenticateRepository @Inject constructor() : AuthenticateRepository {

    override suspend fun loginUser(
        email: String,
        password: String,
    ) = Unit

    override suspend fun loginUserWithQR(qrCode: String) = Unit

    override suspend fun logoutUser() = Unit

    override suspend fun forgotPassword(
        email: String,
        otp: String?,
    ) = Unit
}
