package com.pampoukidis.streamcoretv.feature.login.domain

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class LoginDomainModuleTest {

    @Test
    fun `module resolves login use cases with a repository contract`() {
        val repository = FakeAuthenticateRepository()
        val application = koinApplication {
            modules(
                module {
                    single<AuthenticateRepository> { repository }
                },
                loginDomainModule,
            )
        }

        assertIs<ValidateLoginCredentialsUseCase>(application.koin.get<ValidateLoginCredentialsUseCase>())
        assertIs<LoginWithCredentialsUseCase>(application.koin.get<LoginWithCredentialsUseCase>())

        application.close()
    }

    private class FakeAuthenticateRepository : AuthenticateRepository {
        override val authState: Flow<AuthStateModel> = MutableStateFlow(AuthStateModel.LoggedOut)

        override suspend fun bootstrapAuth(): AppResult<AuthStateModel> {
            return AppResult.Success(AuthStateModel.LoggedOut)
        }

        override suspend fun loginUser(
            identifier: String,
            password: String,
        ): AppResult<Unit> {
            return AppResult.Success(Unit)
        }

        override suspend fun loginUserWithQR(qrCode: String): AppResult<Unit> {
            return AppResult.Success(Unit)
        }

        override suspend fun logoutUser(): AppResult<Unit> {
            return AppResult.Success(Unit)
        }

        override suspend fun forgotPassword(
            email: String,
            otp: String?,
        ): AppResult<Unit> {
            return AppResult.Success(Unit)
        }
    }
}
