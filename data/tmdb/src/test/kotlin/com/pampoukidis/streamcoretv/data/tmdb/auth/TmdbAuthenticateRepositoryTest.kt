package com.pampoukidis.streamcoretv.data.tmdb.auth

import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.data.tmdb.catalog.FakeTmdbApi
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbRequestTokenResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbAuthenticationFailureException
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbErrorMapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TmdbAuthenticateRepositoryTest {

    @Test
    fun `bootstrap without saved session returns logged out without network`() = runTest {
        val api = FakeTmdbApi()
        val subject = repository(api = api)

        val result = subject.bootstrapAuth()

        assertEquals(AppResult.Success(AuthStateModel.LoggedOut), result)
        assertEquals(0, api.accountDetailsCalls)
        assertEquals(0, api.movieAccountStatesCalls)
        assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
    }

    @Test
    fun `bootstrap validates saved session and refreshes account`() = runTest {
        val api = FakeTmdbApi()
        val store = FakeTmdbAuthStore()
        store.saveSession(
            sessionId = "session-id",
            account = null,
        )
        val subject = repository(
            api = api,
            store = store,
            accountId = "548",
        )

        val result = subject.bootstrapAuth()

        val expectedState = AuthStateModel.LoggedIn(
            account = AuthAccountModel(
                id = 548,
                username = "lead",
                displayName = "Lead",
            ),
        )
        assertEquals(AppResult.Success(expectedState), result)
        assertEquals(1, api.movieAccountStatesCalls)
        assertEquals(550, api.lastMovieAccountStatesMovieId)
        assertEquals("session-id", api.lastMovieAccountStatesSessionId)
        assertEquals(1, api.accountDetailsCalls)
        assertEquals("session-id", api.lastAccountSessionId)
        assertEquals("session-id", store.sessionId)
        assertEquals(expectedState.account, store.account)
        assertEquals(expectedState, subject.authState.first())
    }

    @Test
    fun `bootstrap clears session and returns session expired when validation is unauthorized`() =
        runTest {
            val api = FakeTmdbApi().apply {
                failure = TmdbAuthenticationFailureException(
                    backendCode = "INVALID_SESSION",
                    message = "TMDB rejected the saved session.",
                )
            }
            val store = FakeTmdbAuthStore()
            store.saveSession(
                sessionId = "session-id",
                account = null,
            )
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val result = subject.bootstrapAuth()

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.SessionExpired)
            assertNull(store.sessionId)
            assertNull(store.account)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }

    @Test
    fun `bootstrap clears session and returns mapped failure when validation has network failure`() =
        runTest {
            val api = FakeTmdbApi().apply {
                failure = IOException("offline")
            }
            val store = FakeTmdbAuthStore()
            store.saveSession(
                sessionId = "session-id",
                account = null,
            )
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val result = subject.bootstrapAuth()

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
            assertNull(store.sessionId)
            assertNull(store.account)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }

    @Test
    fun `bootstrap validates saved session without account id and stores null account`() =
        runTest {
            val api = FakeTmdbApi()
            val store = FakeTmdbAuthStore()
            store.saveSession(
                sessionId = "session-id",
                account = null,
            )
            val subject = repository(
                api = api,
                store = store,
                accountId = "",
            )

            val result = subject.bootstrapAuth()

            assertEquals(AppResult.Success(AuthStateModel.LoggedIn(account = null)), result)
            assertEquals(1, api.movieAccountStatesCalls)
            assertEquals(0, api.accountDetailsCalls)
            assertEquals("session-id", store.sessionId)
            assertNull(store.account)
            assertEquals(AuthStateModel.LoggedIn(account = null), subject.authState.first())
        }

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

        val expectedState = AuthStateModel.LoggedIn(
            account = AuthAccountModel(
                id = 548,
                username = "lead",
                displayName = "Lead",
            ),
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
        assertEquals(expectedState.account, store.account)
        assertEquals(expectedState, subject.authState.first())
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
        assertEquals(AuthStateModel.LoggedIn(account = null), subject.authState.first())
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
        assertEquals(AuthStateModel.LoggedIn(account = null), subject.authState.first())
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
        assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
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
        assertNull(store.account)
        assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
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
        var sessionId: String? = null
            private set

        var account: AuthAccountModel? = null
            private set

        override suspend fun currentSessionId(): String? {
            return sessionId
        }

        override suspend fun saveSession(
            sessionId: String,
            account: AuthAccountModel?,
        ) {
            this.sessionId = sessionId
            this.account = account
        }

        override suspend fun clear() {
            sessionId = null
            account = null
        }
    }
}
