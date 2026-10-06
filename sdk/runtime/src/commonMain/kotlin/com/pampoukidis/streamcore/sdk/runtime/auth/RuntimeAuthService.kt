package com.pampoukidis.streamcore.sdk.runtime.auth

import com.pampoukidis.streamcore.sdk.api.AuthService
import com.pampoukidis.streamcore.sdk.validation.LoginValidator
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreContextFailureReason
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationIssue
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.runtime.session.ProviderSessionFactory
import com.pampoukidis.streamcore.sdk.runtime.session.AccountSession
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.session.contextFailure
import com.pampoukidis.streamcore.sdk.runtime.session.failure
import com.pampoukidis.streamcore.sdk.runtime.session.invalid
import com.pampoukidis.streamcore.sdk.runtime.session.unsupported
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkLocalRepositories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Authentication workflows, credential reconciliation and explicit session restoration for one SDK instance. */
internal class RuntimeAuthService(
    private val runtimeSession: RuntimeSession,
    private val capabilities: StreamCoreCapabilities,
    private val authentication: AuthProvider,
    private val providerSessions: ProviderSessionFactory,
    private val local: SdkLocalRepositories,
) : AuthService {
    private var contextStorageChecked = false

    override suspend fun restoreSession(): StreamCoreResult<StreamCoreContext> {
        return runtimeSession.authenticationMutex.withLock {
            if (runtimeSession.state.value.isClientClosed) {
                return@withLock failure(StreamCoreError.Closed())
            }
            if (runtimeSession.state.value.isAuthInitialized) {
                return@withLock StreamCoreResult.Success(runtimeSession.context.value)
            }
            val preparation = ensureContextStorageAvailable()
            if (preparation is StreamCoreResult.Failure) {
                return@withLock preparation
            }
            when (val restored = runtimeSession.executeWhileOpen { authentication.restoreSession() }) {
                is StreamCoreResult.Failure -> {
                    if (restored.error is StreamCoreError.SessionExpired) {
                        runtimeSession.executeWhileOpen {
                            runtimeSession.invalidateAccount()
                            StreamCoreResult.Success(Unit)
                        }
                    }
                    restored
                }
                is StreamCoreResult.Success -> {
                    val installation = runtimeSession.executeWhileOpen { installAuthenticationState(restored.value) }
                    when (installation) {
                        is StreamCoreResult.Failure -> installation
                        is StreamCoreResult.Success -> StreamCoreResult.Success(runtimeSession.context.value)
                    }
                }
            }
        }
    }

    override suspend fun login(identifier: String, password: String): StreamCoreResult<Unit> {
        if (!capabilities.credentialsLogin) {
            return unsupported("auth.credentials")
        }
        val validation = LoginValidator.validate(identifier, password)
        val issues = buildList {
            if (validation.identifierError != null) {
                add(StreamCoreValidationIssue(StreamCoreValidationField.Identifier, StreamCoreValidationReason.Required))
            }
            if (validation.passwordError != null) {
                add(StreamCoreValidationIssue(StreamCoreValidationField.Password, StreamCoreValidationReason.Required))
            }
        }
        if (issues.isNotEmpty()) {
            return failure(StreamCoreError.Validation(issues))
        }
        return authenticate { authentication.login(identifier.trim(), password) }
    }
    override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> {
        if (!capabilities.qrLogin) return unsupported("auth.qr")
        if (qrCode.isBlank()) return invalid(StreamCoreValidationField.QrCode, StreamCoreValidationReason.Required)
        return authenticate { authentication.loginWithQr(qrCode.trim()) }
    }
    override suspend fun recoverPassword(email: String, otp: String?): StreamCoreResult<Unit> {
        if (!capabilities.passwordRecovery) return unsupported("auth.passwordRecovery")
        if (email.isBlank()) return invalid(StreamCoreValidationField.Email, StreamCoreValidationReason.Required)
        return runtimeSession.executeWhileOpen { authentication.recoverPassword(email.trim(), otp) }
    }
    override suspend fun logout(): StreamCoreResult<Unit> {
        return runtimeSession.authenticationMutex.withLock {
            if (runtimeSession.state.value.isClientClosed) {
                return@withLock failure(StreamCoreError.Closed())
            }
            val preparation = ensureContextStorageAvailable()
            if (preparation is StreamCoreResult.Failure) {
                return@withLock preparation
            }

            val logoutResult = try {
                runtimeSession.executeWhileOpen { authentication.logout() }
            } catch (cancelled: CancellationException) {
                // A provider can authoritatively clear its accountSession before storage cleanup is cancelled.
                withContext(NonCancellable) {
                    reconcileAuthentication()
                }
                throw cancelled
            }
            val sessionRejected = logoutResult is StreamCoreResult.Failure &&
                logoutResult.error is StreamCoreError.SessionExpired
            if (logoutResult is StreamCoreResult.Success || sessionRejected) {
                val accountId = runtimeSession.state.value.accountSession?.account?.id
                try {
                    runtimeSession.executeWhileOpen {
                        if (logoutResult is StreamCoreResult.Failure) {
                            authentication.invalidateSession()
                        }
                        if (accountId != null) {
                            local.saveSelectedProfile(runtimeSession.configuration, accountId, null)
                        }
                        StreamCoreResult.Success(Unit)
                    }
                } finally {
                    runtimeSession.clearAccount()
                }
            }
            return@withLock logoutResult
        }
    }

    private suspend fun authenticate(block: suspend () -> StreamCoreResult<Unit>): StreamCoreResult<Unit> {
        return runtimeSession.authenticationMutex.withLock {
            if (runtimeSession.state.value.isClientClosed) {
                return@withLock failure(StreamCoreError.Closed())
            }
            val preparation = ensureContextStorageAvailable()
            if (preparation is StreamCoreResult.Failure) {
                return@withLock preparation
            }

            try {
                when (val result = runtimeSession.executeWhileOpen(block)) {
                    is StreamCoreResult.Failure -> {
                        reconcileAuthentication()
                        result
                    }
                    is StreamCoreResult.Success -> runtimeSession.executeWhileOpen { installAuthenticationState(authentication.authState.value) }
                }
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    reconcileAuthentication()
                }
                throw cancelled
            }
        }
    }

    // Retry failed checks; a successful check is sufficient for this client lifetime.
    private suspend fun ensureContextStorageAvailable(): StreamCoreResult<Unit> {
        if (contextStorageChecked) {
            return StreamCoreResult.Success(Unit)
        }
        val result = runtimeSession.executeWhileOpen {
            local.checkContextStorage()
        }
        if (result is StreamCoreResult.Success) {
            contextStorageChecked = true
        }
        return result
    }

    private suspend fun reconcileAuthentication() {
        if (runtimeSession.state.value.isClientClosed) {
            return
        }
        when (val committed = authentication.authState.value) {
            is StreamCoreAuthState.LoggedOut -> runtimeSession.clearAccount()
            is StreamCoreAuthState.LoggedIn -> {
                if (committed.account != runtimeSession.context.value.account) {
                    runtimeSession.executeWhileOpen { installAuthenticationState(committed) }
                }
            }
        }
    }

    private suspend fun installAuthenticationState(authState: StreamCoreAuthState): StreamCoreResult<Unit> {
        if (runtimeSession.state.value.isClientClosed) {
            return failure(StreamCoreError.Closed())
        }
        if (authState is StreamCoreAuthState.LoggedOut) {
            runtimeSession.clearAccount()
            return StreamCoreResult.Success(Unit)
        }

        val account = (authState as StreamCoreAuthState.LoggedIn).account
        if (account.id.isBlank()) {
            return failure(StreamCoreError.Parsing())
        }
        if (runtimeSession.configuration.expectedAccountId != null && runtimeSession.configuration.expectedAccountId != account.id) {
            runtimeSession.invalidateAccount()
            return failure(StreamCoreError.Unauthorized())
        }

        val accountSession = AccountSession(account, providerSessions.create(account))
        runtimeSession.state.update { current ->
            if (current.isClientClosed) {
                current
            } else {
                current.withoutProfileAuthorization().copy(accountSession = accountSession, isAuthInitialized = false)
            }
        }

        // A persisted reference is never an authorization grant in a new client instance.
        local.saveSelectedProfile(runtimeSession.configuration, account.id, null)
        val installed = runtimeSession.state.updateAndGet { current ->
            if (current.accountSession === accountSession && !current.isClientClosed) {
                current.copy(isAuthInitialized = true)
            } else {
                current
            }
        }
        return if (installed.accountSession === accountSession && !installed.isClientClosed) {
            StreamCoreResult.Success(Unit)
        } else {
            contextFailure(StreamCoreContextFailureReason.StaleSession)
        }
    }
}
