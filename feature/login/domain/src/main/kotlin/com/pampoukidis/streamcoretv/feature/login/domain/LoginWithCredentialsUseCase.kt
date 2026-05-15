package com.pampoukidis.streamcoretv.feature.login.domain

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.login.data.LoginCredentials
import javax.inject.Inject

class LoginWithCredentialsUseCase @Inject constructor(
    private val authenticateRepository: AuthenticateRepository,
) {

    suspend operator fun invoke(credentials: LoginCredentials): AppResult<Unit> =
        authenticateRepository.loginUser(
            email = credentials.email,
            password = credentials.password,
        )
}
