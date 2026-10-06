package com.pampoukidis.streamcore.sdk.providers.clientb.auth

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ClientBAuthenticateRepositoryTest {

    @Test
    fun `session restoration without saved session returns logged out`() = runTest {
        val subject = repository()

        assertEquals(StreamCoreResult.Success(StreamCoreAuthState.LoggedOut), subject.restoreSession())
        assertEquals(StreamCoreAuthState.LoggedOut, subject.authState.first())
    }

    @Test
    fun `login persists restored session for a new repository instance`() = runTest {
        val store = FakeClientBAuthStore()
        val subject = repository(store)

        assertEquals(
            StreamCoreResult.Success(Unit),
            subject.login(
                identifier = "lead@streamcore.tv",
                password = "password",
            ),
        )
        assertEquals("clientb:lead@streamcore.tv", assertIs<StreamCoreAuthState.LoggedIn>(subject.authState.first()).account.id)

        val restoredSubject = repository(store)
        val restoredState = restoredSubject.restoreSession()

        assertTrue(restoredState is StreamCoreResult.Success)
        assertTrue((restoredState as StreamCoreResult.Success).value is StreamCoreAuthState.LoggedIn)
        assertEquals("clientb:lead@streamcore.tv", assertIs<StreamCoreAuthState.LoggedIn>(restoredSubject.authState.first()).account.id)
    }

    @Test
    fun `unsupported qr login does not create a session`() = runTest {
        val store = FakeClientBAuthStore()
        val subject = repository(store)

        assertEquals(
            StreamCoreResult.Failure(StreamCoreError.Unsupported("loginUserWithQR")),
            subject.loginWithQr(qrCode = "qr-code"),
        )
        val restoredSubject = repository(store)
        assertTrue(restoredSubject.restoreSession() is StreamCoreResult.Success)
        assertEquals(StreamCoreAuthState.LoggedOut, restoredSubject.authState.first())
    }

    @Test
    fun `saved login flag without account identity cannot restore an account`() = runTest {
        val store = FakeClientBAuthStore(isLoggedInValue = true)
        val subject = repository(store)

        assertEquals(StreamCoreResult.Success(StreamCoreAuthState.LoggedOut), subject.restoreSession())
        assertEquals(StreamCoreAuthState.LoggedOut, subject.authState.first())
        assertTrue(store.isLoggedInValue)
    }

    @Test
    fun `logout clears persisted session`() = runTest {
        val store = FakeClientBAuthStore(isLoggedInValue = true, accountIdValue = "clientb:fixture")
        val subject = repository(store)
        assertTrue(subject.restoreSession() is StreamCoreResult.Success)

        assertEquals(StreamCoreResult.Success(Unit), subject.logout())
        assertEquals(StreamCoreAuthState.LoggedOut, subject.authState.first())
        assertEquals(StreamCoreResult.Success(StreamCoreAuthState.LoggedOut), repository(store).restoreSession())
    }

    @Test
    fun `logout store failure preserves authenticated state`() = runTest {
        val store = FakeClientBAuthStore(isLoggedInValue = true, accountIdValue = "clientb:fixture")
        val subject = repository(store)
        assertTrue(subject.restoreSession() is StreamCoreResult.Success)
        store.failure = RuntimeException("disk unavailable")

        val result = subject.logout()

        assertTrue(result is StreamCoreResult.Failure)
        assertTrue((result as StreamCoreResult.Failure).error is StreamCoreError.Unknown)
        assertTrue(subject.authState.first() is StreamCoreAuthState.LoggedIn)
        assertTrue(store.isLoggedInValue)
    }

    @Test
    fun `unsupported password recovery fails explicitly`() = runTest {
        val subject = repository()

        assertEquals(
            StreamCoreResult.Failure(StreamCoreError.Unsupported("forgotPassword")),
            subject.recoverPassword(
                email = "lead@streamcore.tv",
                otp = null,
            ),
        )
    }

    private fun repository(
        store: FakeClientBAuthStore = FakeClientBAuthStore(),
    ): ClientBAuthenticateRepository {
        return ClientBAuthenticateRepository(authStore = store)
    }

    private class FakeClientBAuthStore(
        var isLoggedInValue: Boolean = false,
        var accountIdValue: String? = null,
    ) : ClientBAuthStore {
        var failure: Throwable? = null

        override suspend fun currentAccountId(): String? {
            failure?.let { throwable -> throw throwable }
            return accountIdValue
        }

        override suspend fun setAccountId(id: String) {
            failure?.let { throwable -> throw throwable }
            accountIdValue = id
            isLoggedInValue = true
        }

        override suspend fun clear() {
            failure?.let { throwable -> throw throwable }
            isLoggedInValue = false
            accountIdValue = null
        }
    }
}
