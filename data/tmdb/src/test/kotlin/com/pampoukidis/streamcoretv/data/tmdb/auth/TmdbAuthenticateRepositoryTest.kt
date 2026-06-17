package com.pampoukidis.streamcoretv.data.tmdb.auth

import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.data.tmdb.catalog.FakeTmdbApi
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbRequestTokenResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbErrorMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbAuthenticateRepositoryTest {

    @Test
    fun `login creates token validates credentials creates session and stores account`() = runTest {
        val api = FakeTmdbApi()
        val store = FakeTmdbAuthStore()
        val subject = repository(
            api = api,
            store = store,
            accountId = "548",
        )

        val result = subject.loginUser(
            identifier = "lead",
            password = "password",
        )

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(1, api.createRequestTokenCalls)
        assertEquals(1, api.validateRequestTokenCalls)
        assertEquals(1, api.createSessionCalls)
        assertEquals("lead", api.lastLoginIdentifier)
        assertEquals("password", api.lastLoginPassword)
        assertEquals("request-token", api.lastValidatedRequestToken)
        assertEquals("validated-token", api.lastSessionRequestToken)
        assertEquals("session-id", store.sessionId)
        assertEquals(
            AuthStateModel.LoggedIn(
                account = AuthAccountModel(
                    id = 548,
                    username = "lead",
                    displayName = "Lead",
                ),
            ),
            store.authState.first(),
        )
    }

    @Test
    fun `login skips account fetch when account id is missing`() = runTest {
        val api = FakeTmdbApi()
        val store = FakeTmdbAuthStore()
        val subject = repository(
            api = api,
            store = store,
            accountId = "",
        )

        val result = subject.loginUser(
            identifier = "lead",
            password = "password",
        )

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(0, api.accountDetailsCalls)
        assertEquals(AuthStateModel.LoggedIn(account = null), store.authState.first())
    }

    @Test
    fun `account fetch failure does not block session persistence`() = runTest {
        val api = FakeTmdbApi().apply {
            accountDetailsResponse = null
        }
        val store = FakeTmdbAuthStore()
        val subject = repository(
            api = api,
            store = store,
            accountId = "548",
        )

        val result = subject.loginUser(
            identifier = "lead",
            password = "password",
        )

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(1, api.accountDetailsCalls)
        assertEquals("session-id", store.sessionId)
        assertEquals(AuthStateModel.LoggedIn(account = null), store.authState.first())
    }

    @Test
    fun `invalid credentials return authentication error and do not persist session`() = runTest {
        val api = FakeTmdbApi().apply {
            validatedTokenResponse = TmdbRequestTokenResponseDto(
                success = false,
                expiresAt = null,
                requestToken = "",
            )
        }
        val store = FakeTmdbAuthStore()
        val subject = repository(
            api = api,
            store = store,
            accountId = "548",
        )

        val result = subject.loginUser(
            identifier = "lead",
            password = "wrong",
        )

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Authentication)
        assertNull(store.sessionId)
        assertEquals(AuthStateModel.LoggedOut, store.authState.first())
    }

    @Test
    fun `logout clears local session`() = runTest {
        val api = FakeTmdbApi()
        val store = FakeTmdbAuthStore()
        store.saveSession(
            sessionId = "session-id",
            account = AuthAccountModel(
                id = 548,
                username = "lead",
                displayName = "Lead",
            ),
        )
        val subject = repository(
            api = api,
            store = store,
            accountId = "548",
        )

        val result = subject.logoutUser()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(1, api.deleteSessionCalls)
        assertEquals("session-id", api.lastDeletedSessionId)
        assertNull(store.sessionId)
        assertEquals(AuthStateModel.LoggedOut, store.authState.first())
    }

    @Test
    fun `secondary auth operations complete`() = runTest {
        val subject = repository()

        assertEquals(
            AppResult.Success(Unit),
            subject.loginUserWithQR(qrCode = "qr-code"),
        )
        assertEquals(
            AppResult.Success(Unit),
            subject.forgotPassword(
                email = "lead@streamcore.tv",
                otp = null,
            ),
        )
    }

    private fun repository(
        api: FakeTmdbApi = FakeTmdbApi(),
        store: FakeTmdbAuthStore = FakeTmdbAuthStore(),
        accountId: String = "",
    ): TmdbAuthenticateRepository {
        return TmdbAuthenticateRepository(
            tmdbApi = api,
            callExecutor = TmdbCallExecutor(errorMapper = TmdbErrorMapper()),
            authStore = store,
            accountId = accountId,
        )
    }

    private class FakeTmdbAuthStore : TmdbAuthStore {
        private val _authState = MutableStateFlow<AuthStateModel>(AuthStateModel.LoggedOut)
        override val authState: Flow<AuthStateModel> = _authState

        var sessionId: String? = null
            private set

        override suspend fun currentSessionId(): String? {
            return sessionId
        }

        override suspend fun saveSession(
            sessionId: String,
            account: AuthAccountModel?,
        ) {
            this.sessionId = sessionId
            _authState.value = AuthStateModel.LoggedIn(account = account)
        }

        override suspend fun clear() {
            sessionId = null
            _authState.value = AuthStateModel.LoggedOut
        }
    }
}
