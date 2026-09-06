package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.FakeTmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbAccountDetailsDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbDeleteSessionResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbRequestTokenResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbSessionResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbAuthenticationFailureException
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbErrorMapper
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TmdbAuthenticateRepositoryTest {

    @Test
    fun `bootstrap read failure is sanitized and does not validate or clear a session`() {
        runTest {
            val api = FakeTmdbApi()
            val store = FakeTmdbAuthStore().apply {
                readFailure = IOException("sensitive storage detail")
            }
            val subject = repository(api = api, store = store)

            val error = (subject.bootstrapAuth() as AppResult.Failure).error

            assertTrue(error is AppError.Unknown)
            assertEquals("BOOTSTRAP_LOCAL_READ_FAILED", error.source?.backendCode)
            assertNull(error.source?.backendMessage)
            assertEquals(0, api.movieAccountStatesCalls)
            assertEquals(0, store.clearCalls)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.value)
        }
    }

    @Test
    fun `bootstrap validated session write failure is sanitized and retained for retry`() {
        runTest {
            val api = FakeTmdbApi()
            val store = FakeTmdbAuthStore().apply {
                saveSession("retained-session", null)
                saveFailure = IOException("sensitive storage detail")
            }
            val subject = repository(api = api, store = store, accountId = "548")

            val error = (subject.bootstrapAuth() as AppResult.Failure).error

            assertTrue(error is AppError.Unknown)
            assertEquals("BOOTSTRAP_LOCAL_WRITE_FAILED", error.source?.backendCode)
            assertNull(error.source?.backendMessage)
            assertEquals("retained-session", store.sessionId)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.value)
            assertEquals(0, api.deleteSessionCalls)

            store.saveFailure = null
            assertTrue(subject.bootstrapAuth() is AppResult.Success)
            assertTrue(subject.authState.value is AuthStateModel.LoggedIn)
        }
    }

    @Test
    fun `bootstrap invalid session clear failure preserves authoritative error and retries local cleanup`() {
        runTest {
            val api = FakeTmdbApi()
            val store = FakeTmdbAuthStore().apply { saveSession("invalid-session", null) }
            val subject = repository(api = api, store = store)
            subject.bootstrapAuth()
            api.failure = TmdbAuthenticationFailureException("INVALID_SESSION", "Session rejected")
            store.clearFailure = IOException("sensitive storage detail")

            val error = (subject.bootstrapAuth() as AppResult.Failure).error

            assertTrue(error is AppError.SessionExpired)
            assertEquals("INVALID_SESSION", error.source?.backendCode)
            assertEquals("invalid-session", store.sessionId)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.value)

            store.clearFailure = null
            api.failure = IllegalStateException("Known invalid session must not be validated again")
            assertEquals(AppResult.Success(AuthStateModel.LoggedOut), subject.bootstrapAuth())
            assertNull(store.sessionId)
            assertEquals(0, api.deleteSessionCalls)
        }
    }

    @Test
    fun `login clears rejected bootstrap session after storage recovers without remote deletion`() {
        runTest {
            val api = FakeTmdbApi().apply {
                failure = TmdbAuthenticationFailureException("INVALID_SESSION", "Session rejected")
                deleteSessionResponse = TmdbDeleteSessionResponseDto(success = false)
            }
            val store = FakeTmdbAuthStore().apply {
                saveSession("rejected-session", null)
                clearFailureOnce = IOException("storage unavailable")
            }
            val subject = repository(api = api, store = store)
            assertTrue((subject.bootstrapAuth() as AppResult.Failure).error is AppError.SessionExpired)
            assertEquals("rejected-session", store.sessionId)
            api.failure = null

            assertEquals(AppResult.Success(Unit), subject.loginUser(identifier = "lead", password = "fixture"))

            assertEquals(0, api.deleteSessionCalls)
            assertEquals(2, store.clearCalls)
            assertEquals("session-id", store.sessionId)
            assertTrue(subject.authState.value is AuthStateModel.LoggedIn)
        }
    }

    @Test
    fun `login and logout retry clear rejected logout session without repeating remote deletion`() {
        runTest {
            val client = HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) })
            try {
                val rejection = ClientRequestException(client.get("https://tmdb.test/session"), "")
                for (retryLogin in listOf(true, false)) {
                    val api = CleanupTrackingTmdbApi(deleteFailure = rejection)
                    val store = FakeTmdbAuthStore().apply {
                        saveSession("rejected-session", null)
                        clearFailureOnce = IOException("storage unavailable")
                    }
                    val subject = repository(api = api, store = store)
                    assertTrue((subject.logoutUser() as AppResult.Failure).error is AppError.Unauthorized)
                    assertEquals("rejected-session", store.sessionId)

                    val result = if (retryLogin) {
                        subject.loginUser(identifier = "lead", password = "fixture")
                    } else {
                        subject.logoutUser()
                    }

                    assertEquals(AppResult.Success(Unit), result)
                    assertEquals(listOf("rejected-session"), api.deletedSessionIds)
                    assertEquals(2, store.clearCalls)
                    assertEquals(if (retryLogin) "session-id" else null, store.sessionId)
                    assertEquals(
                        if (retryLogin) AuthStateModel.LoggedIn(account = null) else AuthStateModel.LoggedOut,
                        subject.authState.value,
                    )
                }
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun `pending rejected session cleanup does not bypass revocation for a different stored id`() {
        runTest {
            val api = FakeTmdbApi().apply {
                failure = TmdbAuthenticationFailureException("INVALID_SESSION", "Session rejected")
                deleteSessionResponse = TmdbDeleteSessionResponseDto(success = false)
            }
            val store = FakeTmdbAuthStore().apply {
                saveSession("rejected-session", null)
                clearFailureOnce = IOException("storage unavailable")
            }
            val subject = repository(api = api, store = store)
            assertTrue(subject.bootstrapAuth() is AppResult.Failure)
            store.saveSession("different-session", null)
            api.failure = null

            val error = (subject.loginUser(identifier = "lead", password = "fixture") as AppResult.Failure).error

            assertTrue(error is AppError.Authentication)
            assertEquals("REPLACE_SESSION_DELETE_FAILED", error.source?.backendCode)
            assertEquals("different-session", api.lastDeletedSessionId)
            assertEquals("different-session", store.sessionId)
            assertEquals(0, api.createSessionCalls)
        }
    }

    @Test
    fun `bootstrap storage cancellation propagates at read write and invalid session clear`() {
        runTest {
            for (operation in listOf("read", "write", "clear")) {
                val cancellation = CancellationException(operation)
                val api = FakeTmdbApi()
                val store = FakeTmdbAuthStore().apply { saveSession("retained-session", null) }
                when (operation) {
                    "read" -> store.readFailure = cancellation
                    "write" -> store.saveFailure = cancellation
                    "clear" -> {
                        store.clearFailure = cancellation
                        api.failure = TmdbAuthenticationFailureException("INVALID_SESSION", "Session rejected")
                    }
                }
                val subject = repository(api = api, store = store)

                assertEquals(cancellation, assertFailsWith<CancellationException> { subject.bootstrapAuth() })
                assertEquals("retained-session", store.sessionId)
                assertEquals(AuthStateModel.LoggedOut, subject.authState.value)
            }
        }
    }

    @Test
    fun `logout retry does not repeat remote deletion after local cleanup fails`() {
        runTest {
            val api = FakeTmdbApi()
            val store = FakeTmdbAuthStore().apply { saveSession("session-id", null) }
            val subject = repository(api = api, store = store)
            subject.bootstrapAuth()
            store.clearFailureOnce = IOException("sensitive storage detail")

            val error = (subject.logoutUser() as AppResult.Failure).error

            assertEquals("LOGOUT_LOCAL_CLEAR_FAILED", error.source?.backendCode)
            assertNull(error.source?.backendMessage)
            assertEquals(1, api.deleteSessionCalls)
            api.failure = IllegalStateException("Repeated remote deletion is unsupported")

            assertEquals(AppResult.Success(Unit), subject.logoutUser())
            assertEquals(1, api.deleteSessionCalls)
            assertNull(store.sessionId)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.value)
        }
    }

    @Test
    fun `reload validates revoked session and clears it without repeating deletion`() {
        runTest {
            val api = FakeTmdbApi()
            val store = FakeTmdbAuthStore().apply { saveSession("session-id", null) }
            val subject = repository(api = api, store = store)
            subject.bootstrapAuth()
            store.clearFailureOnce = IOException("storage unavailable")
            assertTrue(subject.logoutUser() is AppResult.Failure)
            api.failure = TmdbAuthenticationFailureException("INVALID_SESSION", "Session rejected")

            val reloaded = repository(api = api, store = store)
            val error = (reloaded.bootstrapAuth() as AppResult.Failure).error

            assertTrue(error is AppError.SessionExpired)
            assertEquals(1, api.deleteSessionCalls)
            assertNull(store.sessionId)
            assertEquals(AuthStateModel.LoggedOut, reloaded.authState.value)
        }
    }

    @Test
    fun `logout cancellation during cleanup propagates and retry skips revoked remote session`() {
        runTest {
            val api = FakeTmdbApi()
            val store = FakeTmdbAuthStore().apply { saveSession("session-id", null) }
            val subject = repository(api = api, store = store)
            val cancellation = CancellationException("cancel cleanup")
            store.clearFailureOnce = cancellation

            assertEquals(cancellation, assertFailsWith<CancellationException> { subject.logoutUser() })
            api.failure = IllegalStateException("Repeated remote deletion is unsupported")
            assertEquals(AppResult.Success(Unit), subject.logoutUser())
            assertEquals(1, api.deleteSessionCalls)
        }
    }

    @Test
    fun `generic remote logout rejection retains session for retry`() {
        runTest {
            val api = CleanupTrackingTmdbApi(deleteResponse = TmdbDeleteSessionResponseDto(success = false))
            val store = FakeTmdbAuthStore().apply { saveSession("session-id", null) }
            val subject = repository(api = api, store = store)
            subject.bootstrapAuth()

            val error = (subject.logoutUser() as AppResult.Failure).error

            assertTrue(error is AppError.Authentication)
            assertEquals("DELETE_SESSION_FAILED", error.source?.backendCode)
            assertEquals("session-id", store.sessionId)
            assertEquals(0, store.clearCalls)
            assertTrue(subject.authState.value is AuthStateModel.LoggedIn)
        }
    }

    @Test
    fun `unauthorized logout invalidates auth even when local cleanup fails`() {
        runTest {
            val client = HttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) })
            try {
                val rejection = ClientRequestException(client.get("https://tmdb.test/session"), "")
                for (cleanupFails in listOf(false, true)) {
                    val api = FakeTmdbApi()
                    val store = FakeTmdbAuthStore().apply { saveSession("session-id", null) }
                    val subject = repository(api = api, store = store)
                    subject.bootstrapAuth()
                    api.failure = rejection
                    if (cleanupFails) {
                        store.clearFailure = IOException("sensitive storage detail")
                    }

                    val error = (subject.logoutUser() as AppResult.Failure).error

                    assertTrue(error is AppError.Unauthorized)
                    assertEquals(AuthStateModel.LoggedOut, subject.authState.value)
                    assertEquals(1, store.clearCalls)
                    assertEquals(if (cleanupFails) "session-id" else null, store.sessionId)
                }
            } finally {
                client.close()
            }
        }
    }

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
    fun `bootstrap preserves session for retry when validation has network failure`() =
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
            assertEquals("session-id", store.sessionId)
            assertNull(store.account)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }

    @Test
    fun `bootstrap preserves session for retry when validation times out`() {
        runTest {
            val api = FakeTmdbApi().apply {
                failure = HttpRequestTimeoutException("https://tmdb.test", 15_000L)
            }
            val store = FakeTmdbAuthStore().apply {
                saveSession(sessionId = "session-id", account = null)
            }
            val subject = repository(api = api, store = store)

            val result = subject.bootstrapAuth()

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Timeout)
            assertEquals("session-id", store.sessionId)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }
    }

    @Test
    fun `bootstrap preserves session for retry when validation payload cannot be parsed`() {
        runTest {
            val api = FakeTmdbApi().apply {
                failure = SerializationException("bad payload")
            }
            val store = FakeTmdbAuthStore().apply {
                saveSession(sessionId = "session-id", account = null)
            }
            val subject = repository(api = api, store = store)

            val result = subject.bootstrapAuth()

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Parsing)
            assertEquals("session-id", store.sessionId)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }
    }

    @Test
    fun `bootstrap preserves retained session when verified account load is transiently unavailable`() {
        runTest {
            val api = CleanupTrackingTmdbApi(
                accountDetailsFailure = IOException("account unavailable"),
            )
            val store = FakeTmdbAuthStore().apply {
                saveSession(sessionId = "retained-session", account = null)
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val result = subject.bootstrapAuth()

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
            assertEquals("retained-session", store.sessionId)
            assertEquals(0, store.clearCalls)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }
    }

    @Test
    fun `bootstrap clears retained session when verified account id does not match configuration`() {
        runTest {
            val api = CleanupTrackingTmdbApi(
                accountDetailsResponse = TmdbAccountDetailsDto(
                    id = 999,
                    username = "other",
                    displayName = "Other",
                ),
            )
            val store = FakeTmdbAuthStore().apply {
                saveSession(sessionId = "retained-session", account = null)
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val result = subject.bootstrapAuth()

            assertTrue(result is AppResult.Failure)
            val error = (result as AppResult.Failure).error
            assertTrue(error is AppError.SessionExpired)
            assertEquals("ACCOUNT_ID_MISMATCH", error.source?.backendCode)
            assertNull(store.sessionId)
            assertEquals(1, store.clearCalls)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }
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
        assertEquals(0, api.deleteSessionCalls)
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
    fun `login revokes retained session before creating and persisting replacement`() {
        runTest {
            val api = CleanupTrackingTmdbApi()
            val store = FakeTmdbAuthStore().apply {
                saveSession(sessionId = "retained-session", account = null)
            }
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
            assertEquals(
                listOf("deleteSession:retained-session", "createSession"),
                api.events,
            )
            assertEquals("session-id", store.sessionId)
            assertEquals(1, store.clearCalls)
        }
    }

    @Test
    fun `login aborts replacement and preserves retained id when revocation fails`() {
        runTest {
            val api = CleanupTrackingTmdbApi(
                deleteFailure = IOException("revoke unavailable"),
            )
            val store = FakeTmdbAuthStore().apply {
                saveSession(sessionId = "retained-session", account = null)
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val result = subject.loginUser(
                identifier = "lead",
                password = "password",
            )

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
            assertEquals("retained-session", store.sessionId)
            assertEquals(0, store.clearCalls)
            assertEquals(0, api.createSessionCalls)
            assertEquals(listOf("retained-session"), api.deletedSessionIds)
        }
    }

    @Test
    fun `login aborts replacement and preserves retained id when revocation is rejected`() {
        runTest {
            val api = CleanupTrackingTmdbApi(
                deleteResponse = TmdbDeleteSessionResponseDto(success = false),
            )
            val store = FakeTmdbAuthStore().apply {
                saveSession(sessionId = "retained-session", account = null)
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val result = subject.loginUser(
                identifier = "lead",
                password = "password",
            )

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Authentication)
            assertEquals("retained-session", store.sessionId)
            assertEquals(0, store.clearCalls)
            assertEquals(0, api.createSessionCalls)
        }
    }

    @Test
    fun `login retry clears retained id without revoking it twice after local clear failure`() {
        runTest {
            val api = CleanupTrackingTmdbApi()
            val store = FakeTmdbAuthStore().apply {
                saveSession(sessionId = "retained-session", account = null)
                clearFailureOnce = IOException("clear unavailable")
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val firstResult = subject.loginUser(
                identifier = "lead",
                password = "password",
            )

            assertTrue(firstResult is AppResult.Failure)
            assertTrue((firstResult as AppResult.Failure).error is AppError.Network)
            assertEquals("retained-session", store.sessionId)
            assertEquals(listOf("retained-session"), api.deletedSessionIds)
            assertEquals(1, store.clearCalls)
            assertEquals(0, api.createSessionCalls)

            val retryResult = subject.loginUser(
                identifier = "lead",
                password = "password",
            )

            assertEquals(AppResult.Success(Unit), retryResult)
            assertEquals(listOf("retained-session"), api.deletedSessionIds)
            assertEquals(
                listOf("deleteSession:retained-session", "createSession"),
                api.events,
            )
            assertEquals(2, store.clearCalls)
            assertEquals("session-id", store.sessionId)
            assertTrue(subject.authState.first() is AuthStateModel.LoggedIn)
        }
    }

    @Test
    fun `login account failure propagates and deletes uncommitted remote session`() = runTest {
        val api = CleanupTrackingTmdbApi(
            accountDetailsFailure = IOException("account unavailable"),
        )
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

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Network)
        assertEquals(1, api.accountDetailsCalls)
        assertEquals(listOf("session-id"), api.deletedSessionIds)
        assertNull(store.sessionId)
        assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
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
        assertEquals(0, api.deleteSessionCalls)
        assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
    }

    @Test
    fun `login rejects mismatched verified account and deletes uncommitted remote session`() {
        runTest {
            val api = CleanupTrackingTmdbApi(
                accountDetailsResponse = TmdbAccountDetailsDto(
                    id = 999,
                    username = "other",
                    displayName = "Other",
                ),
            )
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

            assertTrue(result is AppResult.Failure)
            val error = (result as AppResult.Failure).error
            assertTrue(error is AppError.Authentication)
            assertEquals("ACCOUNT_ID_MISMATCH", error.source?.backendCode)
            assertEquals(listOf("session-id"), api.deletedSessionIds)
            assertNull(store.sessionId)
        }
    }

    @Test
    fun `login deletes remote session when local persistence fails without masking failure`() {
        runTest {
            val api = CleanupTrackingTmdbApi(
                deleteFailure = IllegalStateException("cleanup failed"),
            )
            val store = FakeTmdbAuthStore().apply {
                saveFailure = IOException("local persistence failed")
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val result = subject.loginUser(
                identifier = "lead",
                password = "password",
            )

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
            assertEquals(1, api.deleteSessionCalls)
            assertEquals("session-id", api.lastDeletedSessionId)
            assertNull(store.sessionId)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }
    }

    @Test
    fun `login preserves primary persistence failure when cleanup response is unsuccessful`() {
        runTest {
            val api = CleanupTrackingTmdbApi(
                deleteResponse = TmdbDeleteSessionResponseDto(success = false),
            )
            val store = FakeTmdbAuthStore().apply {
                saveFailure = IOException("local persistence failed")
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val result = subject.loginUser(
                identifier = "lead",
                password = "password",
            )

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
            assertEquals(listOf("session-id"), api.deletedSessionIds)
            assertNull(store.sessionId)
        }
    }

    @Test
    fun `login promotes committed session to success when local save throws after commit`() {
        runTest {
            val api = CleanupTrackingTmdbApi()
            val store = FakeTmdbAuthStore().apply {
                saveBeforeFailure = true
                saveFailure = IOException("reported after commit")
            }
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
            assertEquals("session-id", store.sessionId)
            assertEquals(0, api.deleteSessionCalls)
            assertTrue(subject.authState.first() is AuthStateModel.LoggedIn)
        }
    }

    @Test
    fun `login cancellation after local save commit does not delete remote session`() {
        runTest {
            val api = CleanupTrackingTmdbApi()
            val store = FakeTmdbAuthStore().apply {
                saveBeforeFailure = true
                cancelOnSave = true
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val login = async {
                subject.loginUser(
                    identifier = "lead",
                    password = "password",
                )
            }
            assertFailsWith<CancellationException> { login.await() }

            assertEquals("session-id", store.sessionId)
            assertEquals(0, api.deleteSessionCalls)
            assertTrue(subject.authState.first() is AuthStateModel.LoggedIn)
        }
    }

    @Test
    fun `login cancellation before local persistence deletes remote session and propagates cancellation`() {
        runTest {
            val api = CleanupTrackingTmdbApi()
            val store = FakeTmdbAuthStore().apply {
                cancelOnSave = true
            }
            val subject = repository(
                api = api,
                store = store,
                accountId = "548",
            )

            val login = async {
                subject.loginUser(
                    identifier = "lead",
                    password = "password",
                )
            }
            assertFailsWith<CancellationException> { login.await() }

            assertEquals(1, api.deleteSessionCalls)
            assertEquals("session-id", api.lastDeletedSessionId)
            assertNull(store.sessionId)
            assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        }
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
    fun `logout remote failure preserves local session and authenticated state`() = runTest {
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
        assertTrue(subject.bootstrapAuth() is AppResult.Success)
        api.failure = IOException("offline")

        val result = subject.logoutUser()

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Network)
        assertEquals("session-id", store.sessionId)
        assertTrue(subject.authState.first() is AuthStateModel.LoggedIn)
    }

    @Test
    fun `logout local clear failure preserves local session and authenticated state`() = runTest {
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
        assertTrue(subject.bootstrapAuth() is AppResult.Success)
        store.clearFailure = IOException("disk unavailable")

        val result = subject.logoutUser()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AppError.Unknown)
        assertEquals("LOGOUT_LOCAL_CLEAR_FAILED", error.source?.backendCode)
        assertEquals("session-id", store.sessionId)
        assertTrue(subject.authState.first() is AuthStateModel.LoggedIn)
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
        api: TmdbApi = FakeTmdbApi(),
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
        var readFailure: Throwable? = null
        var cancelOnSave: Boolean = false
        var clearFailure: Throwable? = null
        var clearFailureOnce: Throwable? = null
        var saveBeforeFailure: Boolean = false
        var saveFailure: Throwable? = null

        var clearCalls: Int = 0
            private set

        var sessionId: String? = null
            private set

        var account: AuthAccountModel? = null
            private set

        override suspend fun currentSessionId(): String? {
            readFailure?.let { throwable -> throw throwable }
            return sessionId
        }

        override suspend fun saveSession(
            sessionId: String,
            account: AuthAccountModel?,
        ) {
            if (saveBeforeFailure) {
                this.sessionId = sessionId
                this.account = account
            }
            if (cancelOnSave) {
                currentCoroutineContext().cancel(CancellationException("cancelled during save"))
                yield()
            }
            saveFailure?.let { throwable -> throw throwable }
            if (!saveBeforeFailure) {
                this.sessionId = sessionId
                this.account = account
            }
        }

        override suspend fun clear() {
            clearCalls += 1
            clearFailureOnce?.let { throwable ->
                clearFailureOnce = null
                throw throwable
            }
            clearFailure?.let { throwable -> throw throwable }
            sessionId = null
            account = null
        }
    }

    private class CleanupTrackingTmdbApi(
        private val delegate: FakeTmdbApi = FakeTmdbApi(),
        private val accountDetailsFailure: Throwable? = null,
        private val accountDetailsResponse: TmdbAccountDetailsDto? = null,
        private val deleteFailure: Throwable? = null,
        private val deleteResponse: TmdbDeleteSessionResponseDto? = null,
    ) : TmdbApi by delegate {

        val deletedSessionIds = mutableListOf<String>()
        val events = mutableListOf<String>()

        var accountDetailsCalls: Int = 0
            private set

        val createSessionCalls: Int
            get() {
                return delegate.createSessionCalls
            }

        val deleteSessionCalls: Int
            get() {
                return deletedSessionIds.size
            }

        val lastDeletedSessionId: String?
            get() {
                return deletedSessionIds.lastOrNull()
            }

        override suspend fun createSession(requestToken: String): TmdbSessionResponseDto {
            events += "createSession"
            return delegate.createSession(requestToken = requestToken)
        }

        override suspend fun getAccountDetails(
            accountId: Int,
            sessionId: String,
        ): TmdbAccountDetailsDto {
            accountDetailsCalls += 1
            accountDetailsFailure?.let { throwable -> throw throwable }
            return accountDetailsResponse ?: delegate.getAccountDetails(
                accountId = accountId,
                sessionId = sessionId,
            )
        }

        override suspend fun deleteSession(sessionId: String): TmdbDeleteSessionResponseDto {
            deletedSessionIds += sessionId
            events += "deleteSession:$sessionId"
            deleteFailure?.let { throwable -> throw throwable }
            return deleteResponse ?: delegate.deleteSession(sessionId = sessionId)
        }
    }
}
