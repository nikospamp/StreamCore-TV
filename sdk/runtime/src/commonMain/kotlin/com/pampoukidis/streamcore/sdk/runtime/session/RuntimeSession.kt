package com.pampoukidis.streamcore.sdk.runtime.session

import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreContextFailureReason
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationIssue
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.runtime.auth.AuthProvider
import com.pampoukidis.streamcore.sdk.runtime.error.ProviderOperationException
import com.pampoukidis.streamcore.sdk.runtime.storage.accountStorageKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

/**
 * One SDK instance's lifetime and current account/profile authorization.
 * Auth/profile services change the atomic state; data services use the guarded operations below.
 * These checks deliberately surround suspending work so old results cannot affect a newer account or profile.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class RuntimeSession(
    val configuration: StreamCoreConfiguration,
    private val authentication: AuthProvider,
    private val closeResources: () -> Unit,
) {
    val state = MutableStateFlow(ClientSessionState())
    val authenticationMutex = Mutex()
    val instanceId = kotlin.random.Random.nextLong().toULong().toString(16)
    private val ownedWork = SupervisorJob()
    val context: StateFlow<StreamCoreContext> = ContextStateFlow(state) { current ->
        StreamCoreContext(
            account = current.accountSession?.account?.takeIf { current.isAuthInitialized },
            profile = current.authorizedProfile?.profile,
            isAuthInitialized = current.isAuthInitialized,
            isClosed = current.isClientClosed,
            profileActivationId = current.authorizedProfile?.id,
        )
    }

    /** Captures authorization now; never reacquire it for an already-created observer or recorder. */
    val currentAuthorization: AuthorizedProfile?
        get() {
            return state.value.authorizedProfile
        }

    fun close() {
        val previous = state.getAndUpdate {
            it.copy(
                accountSession = null,
                authorizedProfile = null,
                profileSelection = null,
                pinChallenge = null,
                pinVerification = null,
                isClientClosed = true
            )
        }
        if (previous.isClientClosed) return
        ownedWork.cancel()
        closeResources()
    }

    fun storageKey(authorization: AuthorizedProfile): String {
        return accountStorageKey(
            configuration,
            authorization.accountSession.account.id,
            authorization.profile.id
        )
    }

    /**
     * Requires an open client with initialized authentication and the same account before and after work.
     * A SessionExpired result revokes only that account; cancellation still propagates to the caller.
     */
    suspend fun <T> withAuthenticatedAccount(
        expectedSession: AccountSession? = state.value.accountSession,
        block: suspend (AccountSession) -> StreamCoreResult<T>,
    ): StreamCoreResult<T> {
        val snapshot = state.value
        if (snapshot.isClientClosed) {
            return failure(StreamCoreError.Closed())
        }
        val accountSession = expectedSession ?: return contextFailure(accountFailure(snapshot))
        if (!snapshot.isAuthInitialized) {
            return contextFailure(StreamCoreContextFailureReason.AuthNotInitialized)
        }
        if (snapshot.accountSession !== accountSession) {
            return contextFailure(StreamCoreContextFailureReason.StaleSession)
        }

        val result = executeWhileOpen { block(accountSession) }
        return reconcileAccountResult(accountSession, result)
    }

    suspend fun <T> reconcileAccountResult(
        accountSession: AccountSession,
        result: StreamCoreResult<T>
    ): StreamCoreResult<T> {
        if (state.value.isClientClosed) {
            return failure(StreamCoreError.Closed())
        }
        if (state.value.accountSession !== accountSession) {
            return contextFailure(StreamCoreContextFailureReason.StaleSession)
        }
        if (result is StreamCoreResult.Failure && result.error is StreamCoreError.SessionExpired) {
            authenticationMutex.withLock {
                if (state.value.accountSession === accountSession) {
                    executeWhileOpen {
                        invalidateAccount()
                        StreamCoreResult.Success(Unit)
                    }
                }
            }
        }
        return result
    }

    /** Account-level profile management: the target must still exist, but need not be the active profile. */
    suspend fun <T> withAccountProfile(
        profileId: String,
        block: suspend (AccountSession, StreamCoreProfile) -> StreamCoreResult<T>
    ): StreamCoreResult<T> {
        if (profileId.isBlank()) return invalid(
            StreamCoreValidationField.ProfileId,
            StreamCoreValidationReason.Required
        )
        return withAuthenticatedAccount { accountSession ->
            when (val profiles = accountSession.providers.profiles.getProfiles()) {
                is StreamCoreResult.Failure -> profiles
                is StreamCoreResult.Success -> {
                    val profile = profiles.value.find { it.id == profileId }
                    if (profile == null) {
                        revokeProfile(accountSession, profileId)
                        contextFailure(StreamCoreContextFailureReason.ProfileUnavailable)
                    } else {
                        block(accountSession, profile)
                    }
                }
            }
        }
    }

    /**
     * Uses the captured profile grant and refreshes its backend profile on every invocation.
     * Deleted/changed profiles revoke or replace that grant. Work and its result must still belong to it.
     * Deliberately preserve this fetch/check order; changing it is a behavior decision, not an optimization here.
     */
    suspend fun <T> withAuthorizedProfile(
        profileId: String,
        expectedAuthorization: AuthorizedProfile? = state.value.authorizedProfile,
        block: suspend (AuthorizedProfile, StreamCoreProfile) -> StreamCoreResult<T>,
    ): StreamCoreResult<T> {
        profileFailure(profileId, expectedAuthorization)?.let { return failure(it) }
        val authorization = checkNotNull(expectedAuthorization)
        val result = executeWhileOpen {
            when (val profiles = authorization.providers.profiles.getProfiles()) {
                is StreamCoreResult.Failure -> profiles
                is StreamCoreResult.Success -> {
                    val profile = profiles.value.find { it.id == profileId }
                    if (profile == null) {
                        revokeProfile(authorization.accountSession, profileId)
                        contextFailure(StreamCoreContextFailureReason.ProfileUnavailable)
                    } else if (profile != authorization.profile) {
                        updateKnownProfile(authorization.accountSession, profile)
                        contextFailure(StreamCoreContextFailureReason.StaleActivation)
                    } else if (!isCurrent(authorization)) {
                        contextFailure(StreamCoreContextFailureReason.StaleActivation)
                    } else {
                        block(authorization, profile)
                    }
                }
            }
        }
        return checkProfileResult(authorization, result)
    }

    suspend fun <T> checkProfileResult(
        authorization: AuthorizedProfile,
        result: StreamCoreResult<T>
    ): StreamCoreResult<T> {
        if (state.value.isClientClosed) {
            return failure(StreamCoreError.Closed())
        }
        if (state.value.accountSession !== authorization.accountSession) {
            return contextFailure(StreamCoreContextFailureReason.StaleSession)
        }
        if (state.value.authorizedProfile !== authorization) {
            if (result is StreamCoreResult.Failure &&
                (result.error as? StreamCoreError.InvalidContext)?.reason == StreamCoreContextFailureReason.ProfileUnavailable
            ) {
                return result
            }
            return contextFailure(StreamCoreContextFailureReason.StaleActivation)
        }
        return reconcileAccountResult(authorization.accountSession, result)
    }

    fun profileFailure(
        profileId: String,
        expectedAuthorization: AuthorizedProfile?,
        current: ClientSessionState = state.value
    ): StreamCoreError? {
        if (current.isClientClosed) return StreamCoreError.Closed()
        if (!current.isAuthInitialized) return StreamCoreError.InvalidContext(
            StreamCoreContextFailureReason.AuthNotInitialized
        )
        if (expectedAuthorization != null && current.accountSession !== expectedAuthorization.accountSession) return StreamCoreError.InvalidContext(
            StreamCoreContextFailureReason.StaleSession
        )
        if (current.accountSession == null) return StreamCoreError.InvalidContext(
            StreamCoreContextFailureReason.Unauthenticated
        )
        if (expectedAuthorization == null) return StreamCoreError.InvalidContext(
            StreamCoreContextFailureReason.NoActiveProfile
        )
        if (expectedAuthorization.profile.id != profileId) return StreamCoreError.InvalidContext(
            StreamCoreContextFailureReason.ProfileMismatch
        )
        if (current.authorizedProfile !== expectedAuthorization) return StreamCoreError.InvalidContext(
            StreamCoreContextFailureReason.StaleActivation
        )
        return null
    }

    fun accountFailure(current: ClientSessionState): StreamCoreContextFailureReason {
        return if (!current.isAuthInitialized) {
            StreamCoreContextFailureReason.AuthNotInitialized
        } else {
            StreamCoreContextFailureReason.Unauthenticated
        }
    }

    fun isCurrent(
        profileSelection: ProfileSelectionAttempt,
        current: ClientSessionState = state.value
    ): Boolean {
        return !current.isClientClosed && current.accountSession === profileSelection.accountSession && current.profileSelection === profileSelection
    }

    fun isCurrent(authorizedProfile: AuthorizedProfile): Boolean {
        return !state.value.isClientClosed && state.value.authorizedProfile === authorizedProfile && state.value.accountSession === authorizedProfile.accountSession
    }

    fun isCurrent(pinVerification: ProfilePinVerification): Boolean {
        return state.value.pinVerification === pinVerification && state.value.pinChallenge === pinVerification.pinChallenge &&
                isCurrent(pinVerification.pinChallenge.profileSelection)
    }

    fun revokeProfile(accountSession: AccountSession, profileId: String) {
        state.update { if (it.accountSession === accountSession && (it.authorizedProfile?.profile?.id == profileId || it.pinChallenge?.model?.profile?.id == profileId)) it.withoutProfileAuthorization() else it }
    }

    fun reconcileAvailableProfiles(
        accountSession: AccountSession,
        profiles: List<StreamCoreProfile>
    ) {
        val snapshot = state.value
        if (snapshot.accountSession !== accountSession) return
        snapshot.authorizedProfile?.let { active ->
            val found = profiles.find { it.id == active.profile.id }
            if (found == null) revokeProfile(
                accountSession,
                active.profile.id
            ) else if (found != active.profile) updateKnownProfile(accountSession, found)
        }
        snapshot.pinChallenge?.let {
            if (profiles.none { profile -> profile.id == it.model.profile.id }) revokeProfile(
                accountSession,
                it.model.profile.id
            )
        }
    }

    fun updateKnownProfile(accountSession: AccountSession, profile: StreamCoreProfile) {
        state.update { current ->
            val active = current.authorizedProfile
            if (current.accountSession !== accountSession) current
            else if (active?.profile?.id == profile.id && active.profile != profile) {
                if (active.profile.pinPolicy != profile.pinPolicy) current.withoutProfileAuthorization()
                else {
                    val selectionVersion = current.selectionVersion + 1
                    val profileSelection = ProfileSelectionAttempt(accountSession, selectionVersion)
                    current.copy(
                        selectionVersion = selectionVersion,
                        profileSelection = profileSelection,
                        authorizedProfile = AuthorizedProfile(
                            "$instanceId:profile:$selectionVersion",
                            profileSelection,
                            profile,
                            active.pinVerification
                        )
                    )
                }
            } else if (current.pinChallenge?.model?.profile?.id == profile.id && current.pinChallenge.model.profile.pinPolicy != profile.pinPolicy) current.withoutProfileAuthorization()
            else current
        }
    }

    suspend fun filterAllowedContents(
        result: StreamCoreResult<List<StreamCoreContent>>,
        authorization: AuthorizedProfile,
        profile: StreamCoreProfile
    ): StreamCoreResult<List<StreamCoreContent>> {
        return when (result) {
            is StreamCoreResult.Failure -> result
            is StreamCoreResult.Success -> StreamCoreResult.Success(result.value.filter {
                authorization.providers.contentPolicy.isContentAllowed(
                    profile,
                    it
                )
            })
        }
    }

    /** Keeps a stored-data observer tied to its original authorization, including across collection/recollection. */
    fun <T> observeStoredProfileData(
        profileId: String,
        authorization: AuthorizedProfile?,
        supported: Boolean,
        operation: String,
        block: suspend (AuthorizedProfile, StreamCoreProfile) -> Flow<StreamCoreResult<T>>
    ): Flow<StreamCoreResult<T>> {
        if (!supported) return flowOf(unsupported(operation))
        // Old observers never attach to a subsequent authorization authorizedProfile.
        return state.map { profileFailure(profileId, authorization, it) }.distinctUntilChanged()
            .flatMapLatest { error ->
                if (error != null) flowOf(failure(error))
                else flow<StreamCoreResult<T>> {
                    val authorizedProfile = checkNotNull(authorization)
                    when (val valid = withAuthorizedProfile(
                        profileId,
                        authorizedProfile
                    ) { _, profile -> StreamCoreResult.Success(profile) }) {
                        is StreamCoreResult.Failure -> emit(valid)
                        is StreamCoreResult.Success -> emitAll(
                            block(
                                authorizedProfile,
                                valid.value
                            ).map { checkProfileResult(authorizedProfile, it) })
                    }
                }.catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    val result = failure(
                        (throwable as? ProviderOperationException)?.error
                            ?: StreamCoreError.Storage()
                    )
                    emit(
                        if (authorization != null) checkProfileResult(
                            authorization,
                            result
                        ) else result
                    )
                }
            }
    }

    /** Cancels work with the caller or SDK close and maps expected provider/serialization failures. */
    suspend fun <T> executeWhileOpen(block: suspend () -> StreamCoreResult<T>): StreamCoreResult<T> {
        if (state.value.isClientClosed) {
            return failure(StreamCoreError.Closed())
        }
        val operation = Job(currentCoroutineContext()[Job])
        val closeRegistration = ownedWork.invokeOnCompletion { operation.cancel() }
        return try {
            withContext(operation) {
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: ProviderOperationException) {
                    StreamCoreResult.Failure(failure.error)
                } catch (_: SerializationException) {
                    StreamCoreResult.Failure(StreamCoreError.Parsing())
                } catch (_: Exception) {
                    StreamCoreResult.Failure(StreamCoreError.Unknown())
                }
            }
        } finally {
            closeRegistration.dispose()
            operation.complete()
        }
    }

    fun clearAccount() {
        state.update {
            it.withoutProfileAuthorization().copy(accountSession = null, isAuthInitialized = true)
        }
    }

    suspend fun invalidateAccount() {
        try {
            authentication.invalidateSession()
        } finally {
            clearAccount()
        }
    }
}

internal fun invalid(
    field: StreamCoreValidationField,
    reason: StreamCoreValidationReason
): StreamCoreResult.Failure {
    return failure(StreamCoreError.Validation(listOf(StreamCoreValidationIssue(field, reason))))
}

internal fun contextFailure(reason: StreamCoreContextFailureReason): StreamCoreResult.Failure {
    return failure(StreamCoreError.InvalidContext(reason))
}

internal fun failure(error: StreamCoreError): StreamCoreResult.Failure {
    return StreamCoreResult.Failure(error)
}

internal fun unsupported(operation: String): StreamCoreResult.Failure {
    return failure(StreamCoreError.Unsupported(operation))
}
