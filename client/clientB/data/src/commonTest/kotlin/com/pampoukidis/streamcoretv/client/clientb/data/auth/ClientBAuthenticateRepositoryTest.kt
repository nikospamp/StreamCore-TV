package com.pampoukidis.streamcoretv.client.clientb.data.auth

import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class ClientBAuthenticateRepositoryTest {

    @Test
    fun `bootstrap without saved session returns logged out`() = runTest {
        val subject = repository()

        assertEquals(AppResult.Success(AuthStateModel.LoggedOut), subject.bootstrapAuth())
        assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
    }

    @Test
    fun `login persists restored session for a new repository instance`() = runTest {
        val store = FakeClientBAuthStore()
        val subject = repository(store)

        assertEquals(
            AppResult.Success(Unit),
            subject.loginUser(
                identifier = "lead@streamcore.tv",
                password = "password",
            ),
        )
        assertTrue(subject.authState.first() is AuthStateModel.LoggedIn)

        val restoredSubject = repository(store)
        val restoredState = restoredSubject.bootstrapAuth()

        assertTrue(restoredState is AppResult.Success)
        assertTrue((restoredState as AppResult.Success).value is AuthStateModel.LoggedIn)
        assertTrue(restoredSubject.authState.first() is AuthStateModel.LoggedIn)
    }

    @Test
    fun `qr login persists restored session`() = runTest {
        val store = FakeClientBAuthStore()
        val subject = repository(store)

        assertEquals(
            AppResult.Success(Unit),
            subject.loginUserWithQR(qrCode = "qr-code"),
        )
        val restoredSubject = repository(store)
        assertTrue(restoredSubject.bootstrapAuth() is AppResult.Success)
        assertTrue(restoredSubject.authState.first() is AuthStateModel.LoggedIn)
    }

    @Test
    fun `logout clears persisted session`() = runTest {
        val store = FakeClientBAuthStore(isLoggedInValue = true)
        val subject = repository(store)
        assertTrue(subject.bootstrapAuth() is AppResult.Success)

        assertEquals(AppResult.Success(Unit), subject.logoutUser())
        assertEquals(AuthStateModel.LoggedOut, subject.authState.first())
        assertEquals(AppResult.Success(AuthStateModel.LoggedOut), repository(store).bootstrapAuth())
    }

    @Test
    fun `logout store failure preserves authenticated state`() = runTest {
        val store = FakeClientBAuthStore(isLoggedInValue = true)
        val subject = repository(store)
        assertTrue(subject.bootstrapAuth() is AppResult.Success)
        store.failure = RuntimeException("disk unavailable")

        val result = subject.logoutUser()

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Unknown)
        assertTrue(subject.authState.first() is AuthStateModel.LoggedIn)
        assertTrue(store.isLoggedInValue)
    }

    @Test
    fun `forgot password completes`() = runTest {
        val subject = repository()

        assertEquals(
            AppResult.Success(Unit),
            subject.forgotPassword(
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
    ) : ClientBAuthStore {
        var failure: Throwable? = null

        override suspend fun isLoggedIn(): Boolean {
            failure?.let { throwable -> throw throwable }
            return isLoggedInValue
        }

        override suspend fun setLoggedIn() {
            failure?.let { throwable -> throw throwable }
            isLoggedInValue = true
        }

        override suspend fun clear() {
            failure?.let { throwable -> throw throwable }
            isLoggedInValue = false
        }
    }
}
