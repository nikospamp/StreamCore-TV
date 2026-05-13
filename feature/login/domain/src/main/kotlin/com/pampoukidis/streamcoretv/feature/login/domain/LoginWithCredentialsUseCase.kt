package com.pampoukidis.streamcoretv.feature.login.domain

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import javax.inject.Inject

class LoginWithCredentialsUseCase @Inject constructor(
    private val authenticateRepository: AuthenticateRepository,
) {

    suspend operator fun invoke(credentials: LoginCredentials) {
        authenticateRepository.loginUser(
            email = credentials.email,
            password = credentials.password,
        )
    }
}
