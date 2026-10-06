package com.pampoukidis.streamcoretv.auth

import com.pampoukidis.streamcore.sdk.api.*
import com.pampoukidis.streamcore.sdk.model.*
import com.pampoukidis.streamcore.sdk.model.profile.*
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppAuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts as loading until session restoration runs`() {
        val repository = FakeSdkClient()

        val subject = AppAuthViewModel(client = repository)

        assertEquals(AppAuthUiState.Loading, subject.uiState.value)
        assertEquals(0, repository.sessionRestorationCalls)
    }

    @Test
    fun `emits ready logged in after valid session restoration`() {
        val authState = StreamCoreAuthState.LoggedIn(
            account = StreamCoreAuthAccount(
                id = "548",
                username = "lead",
                displayName = "Lead",
            ),
        )
        val repository = FakeSdkClient(
            sessionRestorationResult = StreamCoreResult.Success(authState),
        )
        val subject = AppAuthViewModel(client = repository)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                activeProfileId = null,
            ),
            subject.uiState.value,
        )
        assertEquals(1, repository.sessionRestorationCalls)
    }

    @Test
    fun `emits ready logged out and error effect after failed session restoration`() = runTest {
        val error = StreamCoreError.Network()
        val repository = FakeSdkClient(
            sessionRestorationResult = StreamCoreResult.Failure(error),
        )
        val subject = AppAuthViewModel(client = repository)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = StreamCoreAuthState.LoggedOut,
                activeProfileId = null,
            ),
            subject.uiState.value,
        )
        assertEquals(AppAuthEffect.ShowError(error), subject.effects.first())
        assertEquals(1, repository.sessionRestorationCalls)
    }

    @Test
    fun `unexpected session restoration storage exception resolves loading with a sanitized error`() {
        runTest {
            val repository = FakeSdkClient(
                sessionRestorationThrowable = IllegalStateException("sensitive storage detail"),
            )
            val subject = AppAuthViewModel(client = repository)

            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(AppAuthUiState.Ready(authState = StreamCoreAuthState.LoggedOut), subject.uiState.value)
            val error = (subject.effects.first() as AppAuthEffect.ShowError).error
            assertTrue(error is StreamCoreError.Unknown)
            assertEquals("AUTH_BOOTSTRAP_FAILURE", error.source?.backendCode)
            assertNull(error.source?.backendMessage)
            assertEquals(1, repository.sessionRestorationCalls)
        }
    }

    @Test
    fun `session restoration cancellation does not become a ready outcome or error effect`() {
        runTest {
            val repository = FakeSdkClient(
                sessionRestorationThrowable = CancellationException("cancelled session restoration"),
            )
            val subject = AppAuthViewModel(client = repository)
            val effect = async { subject.effects.first() }

            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(AppAuthUiState.Loading, subject.uiState.value)
            assertFalse(effect.isCompleted)
            assertEquals(1, repository.sessionRestorationCalls)
            effect.cancel()
        }
    }

    @Test
    fun `retains active profile in app state`() {
        val authState = StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))
        val repository = FakeSdkClient(
            sessionRestorationResult = StreamCoreResult.Success(authState),
        )
        val subject = AppAuthViewModel(client = repository)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        repository.selectProfile("profile-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                activeProfileId = "profile-1",
            ),
            subject.uiState.value,
        )
    }

    @Test
    fun `request and dismiss logout update confirmation state`() {
        val authState = StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))
        val subject = AppAuthViewModel(
            client = FakeSdkClient(
                sessionRestorationResult = StreamCoreResult.Success(authState),
            ),
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        subject.onAction(AppAuthAction.RequestLogout)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                isLogoutConfirmationVisible = true,
            ),
            subject.uiState.value,
        )

        subject.onAction(AppAuthAction.DismissLogoutConfirmation)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(authState = authState),
            subject.uiState.value,
        )
    }

    @Test
    fun `duplicate logout confirmation submits once while in progress`() {
        val logoutGate = CompletableDeferred<Unit>()
        val repository = FakeSdkClient(
            sessionRestorationResult = StreamCoreResult.Success(StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))),
            logoutGate = logoutGate,
        )
        val subject = AppAuthViewModel(client = repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        subject.onAction(AppAuthAction.RequestLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.logoutCalls)
        assertEquals(true, (subject.uiState.value as AppAuthUiState.Ready).isLogoutInProgress)

        logoutGate.complete(Unit)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `successful logout clears profile and confirmation state`() {
        val repository = FakeSdkClient(
            sessionRestorationResult = StreamCoreResult.Success(StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))),
        )
        val subject = AppAuthViewModel(client = repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        repository.selectProfile("profile-1")
        subject.onAction(AppAuthAction.RequestLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(authState = StreamCoreAuthState.LoggedOut),
            subject.uiState.value,
        )
        assertEquals(1, repository.logoutCalls)
    }

    @Test
    fun `failed logout preserves profile and session and emits error`() = runTest {
        val error = StreamCoreError.Network()
        val authState = StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))
        val repository = FakeSdkClient(
            sessionRestorationResult = StreamCoreResult.Success(authState),
            logoutResult = StreamCoreResult.Failure(error),
        )
        val subject = AppAuthViewModel(client = repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        repository.selectProfile("profile-1")
        subject.onAction(AppAuthAction.RequestLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                activeProfileId = "profile-1",
                isLogoutConfirmationVisible = true,
            ),
            subject.uiState.value,
        )
        assertEquals(AppAuthEffect.ShowError(error), subject.effects.first())
    }

    @Test
    fun `authoritative failed logout clears profile and confirmation and emits primary error`() {
        runTest {
            for (error in listOf(StreamCoreError.Unauthorized(), StreamCoreError.SessionExpired())) {
                val repository = FakeSdkClient(
                    sessionRestorationResult = StreamCoreResult.Success(StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))),
                    logoutResult = StreamCoreResult.Failure(error),
                    logoutAuthState = StreamCoreAuthState.LoggedOut,
                )
                val subject = AppAuthViewModel(client = repository)
                mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
                repository.selectProfile("previous-profile")
                subject.onAction(AppAuthAction.RequestLogout)
                subject.onAction(AppAuthAction.ConfirmLogout)

                mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

                assertEquals(AppAuthUiState.Ready(authState = StreamCoreAuthState.LoggedOut), subject.uiState.value)
                assertEquals(AppAuthEffect.ShowError(error), subject.effects.first())

                repository.auth.login(identifier = "new-user", password = "fixture")
                mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
                assertEquals(
                    AppAuthUiState.Ready(authState = StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))),
                    subject.uiState.value,
                )
            }
        }
    }

    @Test
    fun `generic failed logout keeps confirmation and active profile for retry`() {
        runTest {
            val authState = StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))
            for (error in listOf(StreamCoreError.Authentication(), StreamCoreError.Unknown(), StreamCoreError.Unauthorized())) {
                val repository = FakeSdkClient(
                    sessionRestorationResult = StreamCoreResult.Success(authState),
                    logoutResult = StreamCoreResult.Failure(error),
                )
                val subject = AppAuthViewModel(client = repository)
                mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
                repository.selectProfile("profile-1")
                subject.onAction(AppAuthAction.RequestLogout)
                subject.onAction(AppAuthAction.ConfirmLogout)

                mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

                assertEquals(
                    AppAuthUiState.Ready(
                        authState = authState,
                        activeProfileId = "profile-1",
                        isLogoutConfirmationVisible = true,
                    ),
                    subject.uiState.value,
                )
                assertEquals(AppAuthEffect.ShowError(error), subject.effects.first())
            }
        }
    }

    @Test
    fun `unexpected logout exception clears progress and emits unknown error`() = runTest {
        val authState = StreamCoreAuthState.LoggedIn(account = StreamCoreAuthAccount(id = "fixture", username = "fixture", displayName = null))
        val repository = FakeSdkClient(
            sessionRestorationResult = StreamCoreResult.Success(authState),
            logoutThrowable = IllegalStateException("store failed"),
        )
        val subject = AppAuthViewModel(client = repository)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        subject.onAction(AppAuthAction.RequestLogout)
        subject.onAction(AppAuthAction.ConfirmLogout)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AppAuthUiState.Ready(
                authState = authState,
                isLogoutConfirmationVisible = true,
            ),
            subject.uiState.value,
        )
        val effect = subject.effects.first()
        assertTrue(effect is AppAuthEffect.ShowError)
        assertTrue((effect as AppAuthEffect.ShowError).error is StreamCoreError.Unknown)
    }

    private class FakeSdkClient(
        private val sessionRestorationResult: StreamCoreResult<StreamCoreAuthState> = StreamCoreResult.Success(StreamCoreAuthState.LoggedOut),
        private val sessionRestorationThrowable: Throwable? = null,
        private val logoutResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit),
        private val logoutAuthState: StreamCoreAuthState? = null,
        private val logoutGate: CompletableDeferred<Unit>? = null,
        private val logoutThrowable: Throwable? = null,
    ) : StreamCoreClient {
        private val state = MutableStateFlow(StreamCoreContext())
        override val context: StateFlow<StreamCoreContext> = state
        override val configuration = StreamCoreConfiguration("test", "auth-view-model", persistence = StreamCorePersistenceMode.InMemory)
        override val capabilities = StreamCoreCapabilities()
        var sessionRestorationCalls = 0
            private set
        var logoutCalls = 0
            private set

        fun selectProfile(id: String) {
            state.value = state.value.copy(profile = StreamCoreProfile(
                id, "Viewer", StreamCoreProfileAvatar("avatar", null),
                StreamCoreProfileParentalLevel("all", "All", 100), true, false,
            ))
        }

        override val auth: AuthService = object : AuthService {
            override suspend fun restoreSession(): StreamCoreResult<StreamCoreContext> {
                sessionRestorationCalls += 1
                sessionRestorationThrowable?.let { throw it }
                return when (val result = sessionRestorationResult) {
                    is StreamCoreResult.Success -> {
                        state.value = StreamCoreContext(
                            account = (result.value as? StreamCoreAuthState.LoggedIn)?.account,
                            isAuthInitialized = true,
                        )
                        StreamCoreResult.Success(state.value)
                    }
                    is StreamCoreResult.Failure -> result
                }
            }

            override suspend fun login(identifier: String, password: String): StreamCoreResult<Unit> {
                state.value = StreamCoreContext(
                    account = StreamCoreAuthAccount("fixture", "fixture", null), isAuthInitialized = true,
                )
                return StreamCoreResult.Success(Unit)
            }
            override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> {
                error("Unused")
            }
            override suspend fun recoverPassword(email: String, otp: String?): StreamCoreResult<Unit> {
                error("Unused")
            }
            override suspend fun logout(): StreamCoreResult<Unit> {
                logoutCalls += 1
                logoutGate?.await()
                logoutThrowable?.let { throw it }
                if (logoutAuthState is StreamCoreAuthState.LoggedOut || logoutResult is StreamCoreResult.Success) {
                    state.value = StreamCoreContext(isAuthInitialized = true)
                }
                return logoutResult
            }
        }
        override val profiles: ProfileService get() = error("Unused")
        override val home: HomeService get() = error("Unused")
        override val details: DetailsService get() = error("Unused")
        override val search: SearchService get() = error("Unused")
        override val library: LibraryService get() = error("Unused")
        override val playback: PlaybackService get() = error("Unused")
        override fun close() {}
    }
}
