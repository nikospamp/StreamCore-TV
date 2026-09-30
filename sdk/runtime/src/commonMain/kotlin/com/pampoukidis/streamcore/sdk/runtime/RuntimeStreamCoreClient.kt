package com.pampoukidis.streamcore.sdk.runtime

import com.pampoukidis.streamcore.sdk.api.*
import com.pampoukidis.streamcore.sdk.api.validation.*
import com.pampoukidis.streamcore.sdk.model.*
import com.pampoukidis.streamcore.sdk.model.auth.*
import com.pampoukidis.streamcore.sdk.model.catalog.*
import com.pampoukidis.streamcore.sdk.model.error.*
import com.pampoukidis.streamcore.sdk.model.library.*
import com.pampoukidis.streamcore.sdk.model.playback.*
import com.pampoukidis.streamcore.sdk.model.profile.*
import com.pampoukidis.streamcore.sdk.model.search.StreamCoreSearchInteraction
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.AuthProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderOperationException
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderSessionFactory
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderSessionServices
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkLocalRepositories
import com.pampoukidis.streamcore.sdk.runtime.storage.library.LibraryStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/** All public services share one atomic account/activation state. */
@OptIn(ExperimentalCoroutinesApi::class)
class RuntimeStreamCoreClient(
    override val configuration: StreamCoreConfiguration,
    override val capabilities: StreamCoreCapabilities,
    private val authentication: AuthProvider,
    private val sessions: ProviderSessionFactory,
    private val local: SdkLocalRepositories,
    private val closeResources: () -> Unit = {},
) : StreamCoreClient {
    private val state = MutableStateFlow(RuntimeState())
    override val context: StateFlow<StreamCoreContext> = ContextStateFlow(state) { snapshot ->
        // Installation revokes the old grant immediately, but publishes the account only once
        // its required persistence has completed and account/profile operations can begin.
        StreamCoreContext(
            account = snapshot.session?.account?.takeIf { snapshot.bootstrapped },
            profile = snapshot.activation?.profile,
            isBootstrapped = snapshot.bootstrapped,
            isClosed = snapshot.closed,
            profileActivationId = snapshot.activation?.id,
        )
    }
    private val authenticationMutex = Mutex()
    private val ownedWork = SupervisorJob()
    private val instanceId = Random.nextLong().toULong().toString(16)
    private var localPrepared = false

    override suspend fun bootstrap(): StreamCoreResult<StreamCoreContext> {
        return authenticationMutex.withLock {
            if (state.value.closed) {
                return@withLock failure(StreamCoreError.Closed())
            }
            if (state.value.bootstrapped) {
                return@withLock StreamCoreResult.Success(context.value)
            }
            val preparation = prepareLocalState()
            if (preparation is StreamCoreResult.Failure) {
                return@withLock preparation
            }
            when (val restored = executeOperation { authentication.bootstrapAuth() }) {
                is StreamCoreResult.Failure -> {
                    if (restored.error is StreamCoreError.SessionExpired) {
                        executeOperation {
                            invalidateSession()
                            StreamCoreResult.Success(Unit)
                        }
                    }
                    restored
                }
                is StreamCoreResult.Success -> {
                    val installation = executeOperation { installAuthenticationState(restored.value) }
                    when (installation) {
                        is StreamCoreResult.Failure -> installation
                        is StreamCoreResult.Success -> StreamCoreResult.Success(context.value)
                    }
                }
            }
        }
    }
    override val auth: AuthService = object : AuthService {
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
            return executeOperation { authentication.recoverPassword(email.trim(), otp) }
        }
        override suspend fun logout(): StreamCoreResult<Unit> {
            return authenticationMutex.withLock {
                if (state.value.closed) {
                    return@withLock failure(StreamCoreError.Closed())
                }
                val preparation = prepareLocalState()
                if (preparation is StreamCoreResult.Failure) {
                    return@withLock preparation
                }

                val logoutResult = try {
                    executeOperation { authentication.logout() }
                } catch (cancelled: CancellationException) {
                    // A provider can authoritatively clear its session before storage cleanup is cancelled.
                    withContext(NonCancellable) {
                        reconcileAuthentication()
                    }
                    throw cancelled
                }
                val sessionRejected = logoutResult is StreamCoreResult.Failure &&
                    logoutResult.error is StreamCoreError.SessionExpired
                if (logoutResult is StreamCoreResult.Success || sessionRejected) {
                    val accountId = state.value.session?.account?.id
                    try {
                        executeOperation {
                            if (logoutResult is StreamCoreResult.Failure) {
                                authentication.invalidateSession()
                            }
                            if (accountId != null) {
                                local.saveSelectedProfile(configuration, accountId, null)
                            }
                            StreamCoreResult.Success(Unit)
                        }
                    } finally {
                        clearSession()
                    }
                }
                return@withLock logoutResult
            }
        }
    }
    override val profiles: ProfileService = object : ProfileService {
        override suspend fun beginEntry(): StreamCoreResult<StreamCoreProfileEntryResult> {
            val started = startEntry()
            if (started is StreamCoreResult.Failure) return started
            val entry = (started as StreamCoreResult.Success).value
            return entryOperation(entry) {
                when (val result = entry.session.services.profiles.getProfiles()) {
                    is StreamCoreResult.Failure -> result
                    is StreamCoreResult.Success -> when (result.value.size) {
                        0 -> StreamCoreResult.Success(StreamCoreProfileEntryNoProfiles)
                        1 -> when (val selected = selectLoadedProfile(entry, result.value.single())) {
                            is StreamCoreResult.Failure -> selected
                            is StreamCoreResult.Success -> when (val value = selected.value) {
                                is StreamCoreProfileEntryReady -> StreamCoreResult.Success(value)
                                is StreamCoreProfileEntryPinRequired -> StreamCoreResult.Success(value)
                            }
                        }
                        else -> StreamCoreResult.Success(StreamCoreProfileEntryChooseProfile(result.value))
                    }
                }
            }
        }
        override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> {
            return accountOperation { session ->
                val result = session.services.profiles.getProfiles()
                if (result is StreamCoreResult.Success) reconcileProfiles(session, result.value)
                result
            }
        }
        override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> {
            return accountOperation { it.services.profiles.getProfileEditorOptions() }
        }
        override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> {
            if (!capabilities.profileCreation) return unsupported("profiles.create")
            return accountOperation { session ->
                when (val options = session.services.profiles.getProfileEditorOptions()) {
                    is StreamCoreResult.Failure -> options
                    is StreamCoreResult.Success -> {
                        val issues = profileIssues(profile, options.value)
                        if (issues.isNotEmpty()) failure(StreamCoreError.Validation(issues))
                        else session.services.profiles.createProfile(profile.copy(displayName = profile.displayName.trim()))
                    }
                }
            }
        }
        override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> {
            if (!capabilities.profileUpdate) return unsupported("profiles.update")
            return accountProfileOperation(profile.profileId) { session, _ ->
                when (val options = session.services.profiles.getProfileEditorOptions()) {
                    is StreamCoreResult.Failure -> options
                    is StreamCoreResult.Success -> {
                        val issues = profileIssues(StreamCoreCreateProfile(profile.displayName, profile.avatarId, profile.parentalLevelId), options.value)
                        if (issues.isNotEmpty()) failure(StreamCoreError.Validation(issues))
                        else {
                            val result = session.services.profiles.updateProfile(profile.copy(displayName = profile.displayName.trim()))
                            if (result is StreamCoreResult.Success) updateManagedProfile(session, result.value)
                            result
                        }
                    }
                }
            }
        }
        override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> {
            if (!capabilities.profileDeletion) return unsupported("profiles.delete")
            return accountProfileOperation(profileId) { session, profile ->
                if (!profile.canDelete) return@accountProfileOperation invalid(StreamCoreValidationField.ProfileId, StreamCoreValidationReason.NotAllowed)
                val result = session.services.profiles.deleteProfile(profileId)
                if (result is StreamCoreResult.Success) invalidateProfile(session, profileId)
                result
            }
        }
        override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfileSelectionResult> {
            if (profileId.isBlank()) return invalid(StreamCoreValidationField.ProfileId, StreamCoreValidationReason.Required)
            val started = startEntry()
            if (started is StreamCoreResult.Failure) return started
            val entry = (started as StreamCoreResult.Success).value
            return entryOperation(entry) {
                when (val result = entry.session.services.profiles.getProfiles()) {
                    is StreamCoreResult.Failure -> result
                    is StreamCoreResult.Success -> {
                        val profile = result.value.find { it.id == profileId }
                        if (profile == null) contextFailure(StreamCoreContextFailureReason.ProfileUnavailable)
                        else selectLoadedProfile(entry, profile)
                    }
                }
            }
        }
        override suspend fun confirmPin(challengeId: String, pin: String): StreamCoreResult<StreamCoreProfile> {
            if (!capabilities.profilePinVerification) {
                return unsupported("profiles.verifyPin")
            }

            // Validate the captured challenge and input before starting this verification attempt.
            val challenge = state.value.challenge
            if (challenge == null || challenge.model.challengeId != challengeId || !isCurrent(challenge.entry)) {
                return contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
            }
            if (pin.length != challenge.model.digitCount) {
                return invalid(StreamCoreValidationField.Pin, StreamCoreValidationReason.InvalidLength)
            }
            if (pin.any { it !in '0'..'9' }) {
                return invalid(StreamCoreValidationField.Pin, StreamCoreValidationReason.InvalidFormat)
            }
            val verification = PinVerification(challenge)
            val verificationState = state.updateAndGet { current ->
                if (current.challenge === challenge && isCurrent(challenge.entry, current)) {
                    current.copy(verification = verification)
                } else {
                    current
                }
            }
            if (verificationState.verification !== verification) {
                return contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
            }

            return try {
                val confirmationResult = accountOperation(challenge.entry.session) { session ->
                    // Recheck the provider's profile and PIN policy before authoritative verification.
                    val profilesResult = session.services.profiles.getProfiles()
                    if (profilesResult is StreamCoreResult.Failure) {
                        return@accountOperation profilesResult
                    }
                    val availableProfiles = (profilesResult as StreamCoreResult.Success).value
                    val profile = availableProfiles.find { it.id == challenge.model.profile.id }
                    if (profile == null || profile.pinPolicy != challenge.model.profile.pinPolicy) {
                        invalidateEntry(challenge.entry)
                        return@accountOperation contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                    }
                    if (!isCurrent(verification)) {
                        return@accountOperation contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                    }

                    val verificationResult = session.services.profiles.verifyProfilePin(profile.id, pin)
                    if (verificationResult is StreamCoreResult.Failure) {
                        return@accountOperation verificationResult
                    }
                    currentCoroutineContext().ensureActive()
                    if (!isCurrent(verification)) {
                        return@accountOperation contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                    }

                    // Select only after verification, then persist selection before publishing activation.
                    val selectionResult = session.services.profiles.selectProfile(profile.id)
                    if (selectionResult is StreamCoreResult.Failure) {
                        return@accountOperation selectionResult
                    }
                    val selectedProfile = (selectionResult as StreamCoreResult.Success).value
                    if (selectedProfile.id != profile.id || selectedProfile.pinPolicy != profile.pinPolicy) {
                        invalidateEntry(challenge.entry)
                        return@accountOperation contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                    }
                    activate(challenge.entry, selectedProfile, verification)
                }

                // Cancellation/replacement can race with selection or activation; reconcile the final result.
                when {
                    confirmationResult is StreamCoreResult.Failure &&
                        confirmationResult.error is StreamCoreError.SessionExpired -> confirmationResult
                    confirmationResult is StreamCoreResult.Success &&
                        state.value.activation?.verification === verification -> confirmationResult
                    state.value.verification === verification && isCurrent(verification) -> confirmationResult
                    else -> contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                }
            } finally {
                state.update { current ->
                    if (current.verification === verification) {
                        current.copy(verification = null)
                    } else {
                        current
                    }
                }
            }
        }
        override fun cancelPin(challengeId: String): StreamCoreResult<Unit> {
            if (state.value.closed) return failure(StreamCoreError.Closed())
            val challenge = state.value.challenge
            if (challenge == null || challenge.model.challengeId != challengeId) return contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
            state.update {
                // Confirmation may have committed after the challenge was captured above.
                if (it.challenge === challenge || it.activation?.verification?.challenge === challenge) {
                    it.withoutSelection()
                } else {
                    it
                }
            }
            return StreamCoreResult.Success(Unit)
        }
        override suspend fun clearSelection(): StreamCoreResult<Unit> {
            // Revoke memory immediately, before waiting for persistence.
            val previous = state.getAndUpdate { if (it.closed) it else it.withoutSelection() }
            if (previous.closed) return failure(StreamCoreError.Closed())
            val session = previous.session ?: return contextFailure(accountFailure(previous))
            return accountOperation(session) {
                local.saveSelectedProfile(configuration, session.account.id, null)
                StreamCoreResult.Success(Unit)
            }
        }
    }

    override val home: HomeService = object : HomeService {
        override suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>> {
            return profileOperation(profileId) { activation, profile ->
                val services = activation.entry.session.services
                when (val result = services.home.getCollections(profileId)) {
                    is StreamCoreResult.Failure -> result
                    is StreamCoreResult.Success -> StreamCoreResult.Success(result.value.map { row -> row.copy(content = row.content.filter { services.contentPolicy.isContentAllowed(profile, it) }) })
                }
            }
        }
    }
    override val details: DetailsService = object : DetailsService {
        override suspend fun getDetails(profileId: String, contentId: String): StreamCoreResult<StreamCoreContent> {
            if (contentId.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
            return profileOperation(profileId) { activation, profile ->
                val services = activation.entry.session.services
                when (val result = services.details.getDetails(profileId, contentId)) {
                    is StreamCoreResult.Failure -> result
                    is StreamCoreResult.Success -> if (services.contentPolicy.isContentAllowed(profile, result.value)) result else failure(StreamCoreError.Unauthorized())
                }
            }
        }
        override suspend fun getRecommendations(profileId: String, contentId: String): StreamCoreResult<List<StreamCoreContent>> {
            if (contentId.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
            return profileOperation(profileId) { activation, profile -> filterContents(activation.entry.session.services.details.getRecommendations(profileId, contentId), activation, profile) }
        }
    }
    override val playback: PlaybackService = object : PlaybackService {
        override suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCoreResult<StreamCorePlaybackMedia> {
            if (capabilities.playback == StreamCorePlaybackSupport.Unsupported) return unsupported("playback.sources")
            if (request.contentId.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
            if (request.contentSnapshot.id != request.contentId) return invalid(StreamCoreValidationField.ContentSnapshot, StreamCoreValidationReason.Mismatch)
            return profileOperation(request.profileId) { activation, profile ->
                val services = activation.entry.session.services
                when (val content = services.details.getDetails(profile.id, request.contentId)) {
                    is StreamCoreResult.Failure -> content
                    is StreamCoreResult.Success -> {
                        if (!services.contentPolicy.isContentAllowed(profile, content.value)) failure(StreamCoreError.Unauthorized())
                        else if (!isCurrent(activation)) contextFailure(StreamCoreContextFailureReason.StaleActivation)
                        else StreamCoreResult.Success(services.playback.resolveSource(request.copy(contentSnapshot = content.value)))
                    }
                }
            }
        }
        override fun createProgressRecorder(request: StreamCorePlaybackRequest, initialPositionMillis: Long): PlaybackProgressRecorder {
            return RuntimePlaybackProgressRecorder(
                request,
                progressForActivation(state.value.activation),
                initialPositionMillis,
            )
        }
        override fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
            return this@RuntimeStreamCoreClient.observeProgress(profileId, state.value.activation)
        }
        override suspend fun getProgress(profileId: String, contentId: String): StreamCoreResult<StreamCorePlaybackProgressEntry?> {
            return this@RuntimeStreamCoreClient.getProgress(profileId, contentId, state.value.activation)
        }
        override suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit> {
            return updateProgressForActivation(entry, state.value.activation)
        }
        override suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit> {
            return this@RuntimeStreamCoreClient.removeProgress(profileId, contentId, state.value.activation)
        }
    }
    override val search: SearchService = object : SearchService {
        override fun observeHistory(profileId: String): Flow<StreamCoreResult<List<String>>> {
            return observeLocal(profileId, state.value.activation, capabilities.searchHistory, "search.history") { activation, _ ->
                local.history.observe(storageKey(activation)).map { StreamCoreResult.Success(it) }
            }
        }
        override suspend fun recordHistory(profileId: String, query: String): StreamCoreResult<Unit> {
            return this@RuntimeStreamCoreClient.recordHistory(profileId, query, state.value.activation)
        }
        override suspend fun removeHistoryQuery(profileId: String, query: String): StreamCoreResult<Unit> {
            return historyMutation(profileId, state.value.activation) { activation ->
                local.history.remove(storageKey(activation), SearchQueryNormalizer.normalize(query))
            }
        }
        override suspend fun clearHistory(profileId: String): StreamCoreResult<Unit> {
            return historyMutation(profileId, state.value.activation) { activation -> local.history.clear(storageKey(activation)) }
        }
        override suspend fun search(profileId: String, query: String, interaction: StreamCoreSearchInteraction): StreamCoreResult<List<StreamCoreContent>> {
            val captured = state.value.activation
            val result = searchForActivation(profileId, query, captured)
            // profileOperation already validated and applied authoritative rejection to context.
            if (result is StreamCoreResult.Failure) {
                return result
            }
            currentCoroutineContext().ensureActive()
            if (result is StreamCoreResult.Success && interaction != StreamCoreSearchInteraction.Typing && result.value.isNotEmpty()) {
                // Retain successful catalogue results even when a history checkpoint fails.
                this@RuntimeStreamCoreClient.recordHistory(profileId, query, captured)
            }
            return if (captured == null) {
                result
            } else {
                checkedActivationResult(captured, result)
            }
        }
        override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> {
            if (!capabilities.search) return unsupported("search")
            return profileOperation(profileId) { activation, profile -> filterContents(activation.entry.session.services.search.loadTrending(profileId), activation, profile) }
        }
        override suspend fun displayedResults(profileId: String, query: String, results: List<StreamCoreContent>, interaction: StreamCoreSearchInteraction): StreamCoreResult<Unit> {
            val captured = state.value.activation
            return profileOperation(profileId, captured) { _, _ ->
                if (interaction == StreamCoreSearchInteraction.Typing || results.isEmpty()) {
                    StreamCoreResult.Success(Unit)
                } else {
                    this@RuntimeStreamCoreClient.recordHistory(profileId, query, captured)
                }
            }
        }
        override suspend fun resultSelected(profileId: String, query: String): StreamCoreResult<Unit> {
            return this@RuntimeStreamCoreClient.recordHistory(profileId, query, state.value.activation)
        }
    }
    override val library: LibraryService = object : LibraryService {
        override fun observe(profileId: String): Flow<StreamCoreResult<StreamCoreLibrary>> {
            val captured = state.value.activation
            return libraryForActivation(captured).observe(profileId).map { result ->
                if (captured == null) {
                    result
                } else {
                    checkedActivationResult(captured, result)
                }
            }
        }
        override fun observeContentState(profileId: String, contentId: String): Flow<StreamCoreResult<StreamCoreContentLibraryState>> {
            return libraryForActivation(state.value.activation).observeContentState(profileId, contentId)
        }
        override suspend fun setLiked(profileId: String, content: StreamCoreContent, isLiked: Boolean): StreamCoreResult<Unit> {
            return libraryForActivation(state.value.activation).setLiked(profileId, content, isLiked)
        }
        override suspend fun setInMyList(profileId: String, content: StreamCoreContent, isInMyList: Boolean): StreamCoreResult<Unit> {
            return libraryForActivation(state.value.activation).setInMyList(profileId, content, isInMyList)
        }
    }
    override fun close() {
        val previous = state.getAndUpdate { it.copy(session = null, activation = null, entry = null, challenge = null, verification = null, closed = true) }
        if (previous.closed) return
        ownedWork.cancel()
        closeResources()
    }

    private suspend fun authenticate(block: suspend () -> StreamCoreResult<Unit>): StreamCoreResult<Unit> {
        return authenticationMutex.withLock {
            if (state.value.closed) {
                return@withLock failure(StreamCoreError.Closed())
            }
            val preparation = prepareLocalState()
            if (preparation is StreamCoreResult.Failure) {
                return@withLock preparation
            }

            try {
                when (val result = executeOperation(block)) {
                    is StreamCoreResult.Failure -> {
                        reconcileAuthentication()
                        result
                    }
                    is StreamCoreResult.Success -> executeOperation { installAuthenticationState(authentication.authState.value) }
                }
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    reconcileAuthentication()
                }
                throw cancelled
            }
        }
    }

    private suspend fun prepareLocalState(): StreamCoreResult<Unit> {
        if (localPrepared) {
            return StreamCoreResult.Success(Unit)
        }
        val result = executeOperation {
            local.migrateLegacy(configuration, configuration.legacyAccountId ?: authentication.legacyAccountId())
        }
        if (result is StreamCoreResult.Success) {
            localPrepared = true
        }
        return result
    }

    private suspend fun reconcileAuthentication() {
        if (state.value.closed) {
            return
        }
        when (val committed = authentication.authState.value) {
            is StreamCoreAuthState.LoggedOut -> clearSession()
            is StreamCoreAuthState.LoggedIn -> {
                if (committed.account != context.value.account) {
                    executeOperation { installAuthenticationState(committed) }
                }
            }
        }
    }

    private suspend fun installAuthenticationState(authState: StreamCoreAuthState): StreamCoreResult<Unit> {
        if (state.value.closed) {
            return failure(StreamCoreError.Closed())
        }
        if (authState is StreamCoreAuthState.LoggedOut) {
            clearSession()
            return StreamCoreResult.Success(Unit)
        }

        val account = (authState as StreamCoreAuthState.LoggedIn).account
        if (account.id.isBlank()) {
            return failure(StreamCoreError.Parsing())
        }
        if (configuration.expectedAccountId != null && configuration.expectedAccountId != account.id) {
            invalidateSession()
            return failure(StreamCoreError.Unauthorized())
        }

        val session = CapturedSession(account, sessions.create(account))
        state.update { current ->
            if (current.closed) {
                current
            } else {
                current.withoutSelection().copy(session = session, bootstrapped = false)
            }
        }

        // A persisted reference is never an authorization grant in a new client instance.
        local.saveSelectedProfile(configuration, account.id, null)
        val installed = state.updateAndGet { current ->
            if (current.session === session && !current.closed) {
                current.copy(bootstrapped = true)
            } else {
                current
            }
        }
        return if (installed.session === session && !installed.closed) {
            StreamCoreResult.Success(Unit)
        } else {
            contextFailure(StreamCoreContextFailureReason.StaleSession)
        }
    }

    private fun clearSession() {
        state.update { it.withoutSelection().copy(session = null, bootstrapped = true) }
    }

    private suspend fun invalidateSession() {
        try {
            authentication.invalidateSession()
        } finally {
            clearSession()
        }
    }

    private fun startEntry(): StreamCoreResult<EntryAttempt> {
        val snapshot = state.value
        if (snapshot.closed) return failure(StreamCoreError.Closed())
        val session = snapshot.session ?: return contextFailure(accountFailure(snapshot))
        if (!snapshot.bootstrapped) return contextFailure(StreamCoreContextFailureReason.NotBootstrapped)
        val entered = state.updateAndGet { current ->
            if (current.session === session && !current.closed) {
                val generation = current.generation + 1
                current.copy(activation = null, challenge = null, verification = null, generation = generation, entry = EntryAttempt(session, generation))
            } else current
        }
        val entry = entered.entry
        return if (entry != null && entry.session === session && !entered.closed) StreamCoreResult.Success(entry) else contextFailure(StreamCoreContextFailureReason.StaleSession)
    }
    private suspend fun <T> entryOperation(entry: EntryAttempt, block: suspend () -> StreamCoreResult<T>): StreamCoreResult<T> {
        return try {
            val result = accountOperation(entry.session) {
                if (!isCurrent(entry)) {
                    contextFailure(StreamCoreContextFailureReason.StaleActivation)
                } else {
                    block()
                }
            }
            if (result is StreamCoreResult.Failure && result.error is StreamCoreError.SessionExpired) {
                result
            } else if (isCurrent(entry)) {
                result
            } else {
                contextFailure(StreamCoreContextFailureReason.StaleActivation)
            }
        } catch (cancelled: CancellationException) {
            invalidateEntry(entry)
            throw cancelled
        }
    }
    private suspend fun selectLoadedProfile(entry: EntryAttempt, profile: StreamCoreProfile): StreamCoreResult<StreamCoreProfileSelectionResult> {
        if (!isCurrent(entry)) return contextFailure(StreamCoreContextFailureReason.StaleActivation)
        if (profile.pinPolicy != null) return issueChallenge(entry, profile)
        return when (val selected = entry.session.services.profiles.selectProfile(profile.id)) {
            is StreamCoreResult.Failure -> selected
            is StreamCoreResult.Success -> {
                if (selected.value.id != profile.id) return failure(StreamCoreError.Parsing())
                if (selected.value.pinPolicy != null) return issueChallenge(entry, selected.value)
                when (val activated = activate(entry, selected.value, null)) {
                    is StreamCoreResult.Failure -> activated
                    is StreamCoreResult.Success -> StreamCoreResult.Success(StreamCoreProfileEntryReady(activated.value))
                }
            }
        }
    }
    private fun issueChallenge(entry: EntryAttempt, profile: StreamCoreProfile): StreamCoreResult<StreamCoreProfileSelectionResult> {
        if (!capabilities.profilePinVerification) return unsupported("profiles.verifyPin")
        val policy = profile.pinPolicy ?: return failure(StreamCoreError.Parsing())
        val challenge = PinChallenge(entry, StreamCoreProfilePinChallenge("$instanceId:pin:${entry.generation}", profile, policy.digitCount))
        val issued = state.updateAndGet { if (isCurrent(entry, it)) it.copy(challenge = challenge, verification = null) else it }
        return if (issued.challenge === challenge) StreamCoreResult.Success(StreamCoreProfileEntryPinRequired(challenge.model)) else contextFailure(StreamCoreContextFailureReason.StaleActivation)
    }
    private suspend fun activate(entry: EntryAttempt, profile: StreamCoreProfile, verification: PinVerification?): StreamCoreResult<StreamCoreProfile> {
        if (!isCurrent(entry) || verification != null && !isCurrent(verification)) {
            return contextFailure(StreamCoreContextFailureReason.StaleActivation)
        }

        local.saveSelectedProfile(configuration, entry.session.account.id, profile.id)
        currentCoroutineContext().ensureActive()
        val activation = CapturedActivation("$instanceId:profile:${entry.generation}", entry, profile, verification)
        val activated = state.updateAndGet { current ->
            if (isCurrent(entry, current) && (verification == null || current.verification === verification)) {
                current.copy(activation = activation, challenge = null, verification = null)
            } else {
                current
            }
        }
        return if (activated.activation === activation) {
            StreamCoreResult.Success(profile)
        } else {
            contextFailure(StreamCoreContextFailureReason.StaleActivation)
        }
    }

    private fun invalidateEntry(entry: EntryAttempt) {
        state.update { current ->
            if (current.entry === entry) {
                current.withoutSelection()
            } else {
                current
            }
        }
    }
    private fun invalidateProfile(session: CapturedSession, profileId: String) {
        state.update { if (it.session === session && (it.activation?.profile?.id == profileId || it.challenge?.model?.profile?.id == profileId)) it.withoutSelection() else it }
    }
    private fun reconcileProfiles(session: CapturedSession, profiles: List<StreamCoreProfile>) {
        val snapshot = state.value
        if (snapshot.session !== session) return
        snapshot.activation?.let { active ->
            val found = profiles.find { it.id == active.profile.id }
            if (found == null) invalidateProfile(session, active.profile.id) else if (found != active.profile) updateManagedProfile(session, found)
        }
        snapshot.challenge?.let { if (profiles.none { profile -> profile.id == it.model.profile.id }) invalidateProfile(session, it.model.profile.id) }
    }
    private fun updateManagedProfile(session: CapturedSession, profile: StreamCoreProfile) {
        state.update { current ->
            val active = current.activation
            if (current.session !== session) current
            else if (active?.profile?.id == profile.id && active.profile != profile) {
                if (active.profile.pinPolicy != profile.pinPolicy) current.withoutSelection()
                else {
                    val generation = current.generation + 1
                    val entry = EntryAttempt(session, generation)
                    current.copy(generation = generation, entry = entry, activation = CapturedActivation("$instanceId:profile:$generation", entry, profile, active.verification))
                }
            } else if (current.challenge?.model?.profile?.id == profile.id && current.challenge.model.profile.pinPolicy != profile.pinPolicy) current.withoutSelection()
            else current
        }
    }

    private suspend fun <T> accountOperation(
        expectedSession: CapturedSession? = state.value.session,
        block: suspend (CapturedSession) -> StreamCoreResult<T>,
    ): StreamCoreResult<T> {
        val snapshot = state.value
        if (snapshot.closed) {
            return failure(StreamCoreError.Closed())
        }
        val session = expectedSession ?: return contextFailure(accountFailure(snapshot))
        if (!snapshot.bootstrapped) {
            return contextFailure(StreamCoreContextFailureReason.NotBootstrapped)
        }
        if (snapshot.session !== session) {
            return contextFailure(StreamCoreContextFailureReason.StaleSession)
        }

        val result = executeOperation { block(session) }
        return reconcileSessionResult(session, result)
    }
    private suspend fun <T> reconcileSessionResult(session: CapturedSession, result: StreamCoreResult<T>): StreamCoreResult<T> {
        if (state.value.closed) {
            return failure(StreamCoreError.Closed())
        }
        if (state.value.session !== session) {
            return contextFailure(StreamCoreContextFailureReason.StaleSession)
        }
        if (result is StreamCoreResult.Failure && result.error is StreamCoreError.SessionExpired) {
            authenticationMutex.withLock {
                if (state.value.session === session) {
                    executeOperation {
                        invalidateSession()
                        StreamCoreResult.Success(Unit)
                    }
                }
            }
        }
        return result
    }
    private suspend fun <T> accountProfileOperation(profileId: String, block: suspend (CapturedSession, StreamCoreProfile) -> StreamCoreResult<T>): StreamCoreResult<T> {
        if (profileId.isBlank()) return invalid(StreamCoreValidationField.ProfileId, StreamCoreValidationReason.Required)
        return accountOperation { session ->
            when (val profiles = session.services.profiles.getProfiles()) {
                is StreamCoreResult.Failure -> profiles
                is StreamCoreResult.Success -> {
                    val profile = profiles.value.find { it.id == profileId }
                    if (profile == null) {
                        invalidateProfile(session, profileId)
                        contextFailure(StreamCoreContextFailureReason.ProfileUnavailable)
                    } else {
                        block(session, profile)
                    }
                }
            }
        }
    }
    private suspend fun <T> profileOperation(
        profileId: String,
        expected: CapturedActivation? = state.value.activation,
        block: suspend (CapturedActivation, StreamCoreProfile) -> StreamCoreResult<T>,
    ): StreamCoreResult<T> {
        profileFailure(profileId, expected)?.let { return failure(it) }
        val captured = checkNotNull(expected)
        val result = executeOperation {
            when (val profiles = captured.entry.session.services.profiles.getProfiles()) {
                is StreamCoreResult.Failure -> profiles
                is StreamCoreResult.Success -> {
                    val profile = profiles.value.find { it.id == profileId }
                    if (profile == null) {
                        invalidateProfile(captured.entry.session, profileId)
                        contextFailure(StreamCoreContextFailureReason.ProfileUnavailable)
                    } else if (profile != captured.profile) {
                        updateManagedProfile(captured.entry.session, profile)
                        contextFailure(StreamCoreContextFailureReason.StaleActivation)
                    } else if (!isCurrent(captured)) {
                        contextFailure(StreamCoreContextFailureReason.StaleActivation)
                    } else {
                        block(captured, profile)
                    }
                }
            }
        }
        return checkedActivationResult(captured, result)
    }
    private suspend fun <T> checkedActivationResult(captured: CapturedActivation, result: StreamCoreResult<T>): StreamCoreResult<T> {
        if (state.value.closed) {
            return failure(StreamCoreError.Closed())
        }
        if (state.value.session !== captured.entry.session) {
            return contextFailure(StreamCoreContextFailureReason.StaleSession)
        }
        if (state.value.activation !== captured) {
            if (result is StreamCoreResult.Failure &&
                (result.error as? StreamCoreError.InvalidContext)?.reason == StreamCoreContextFailureReason.ProfileUnavailable
            ) {
                return result
            }
            return contextFailure(StreamCoreContextFailureReason.StaleActivation)
        }
        return reconcileSessionResult(captured.entry.session, result)
    }
    private fun profileFailure(profileId: String, expected: CapturedActivation?, current: RuntimeState = state.value): StreamCoreError? {
        if (current.closed) return StreamCoreError.Closed()
        if (!current.bootstrapped) return StreamCoreError.InvalidContext(StreamCoreContextFailureReason.NotBootstrapped)
        if (expected != null && current.session !== expected.entry.session) return StreamCoreError.InvalidContext(StreamCoreContextFailureReason.StaleSession)
        if (current.session == null) return StreamCoreError.InvalidContext(StreamCoreContextFailureReason.Unauthenticated)
        if (expected == null) return StreamCoreError.InvalidContext(StreamCoreContextFailureReason.NoActiveProfile)
        if (expected.profile.id != profileId) return StreamCoreError.InvalidContext(StreamCoreContextFailureReason.ProfileMismatch)
        if (current.activation !== expected) return StreamCoreError.InvalidContext(StreamCoreContextFailureReason.StaleActivation)
        return null
    }
    private fun accountFailure(current: RuntimeState): StreamCoreContextFailureReason {
        return if (!current.bootstrapped) {
            StreamCoreContextFailureReason.NotBootstrapped
        } else {
            StreamCoreContextFailureReason.Unauthenticated
        }
    }

    private fun isCurrent(entry: EntryAttempt, current: RuntimeState = state.value): Boolean {
        return !current.closed && current.session === entry.session && current.entry === entry
    }

    private fun isCurrent(activation: CapturedActivation): Boolean {
        return !state.value.closed && state.value.activation === activation && state.value.session === activation.entry.session
    }

    private fun isCurrent(verification: PinVerification): Boolean {
        return state.value.verification === verification && state.value.challenge === verification.challenge &&
            isCurrent(verification.challenge.entry)
    }

    private fun storageKey(captured: CapturedActivation): String {
        return accountStorageKey(configuration, captured.entry.session.account.id, captured.profile.id)
    }

    private fun libraryForActivation(captured: CapturedActivation?): LibraryService {
        // Bind authorization and partitioning to the facade's captured activation. The service
        // owns its injectable clock and supplies the mutation timestamp before these checks run.
        val authorizedEntries = object : LibraryStore {
            override fun observe(profileId: String): Flow<StreamCoreResult<List<StreamCoreLibraryEntry>>> {
                return observeLocal(profileId, captured, capabilities.localLibrary, "library") { activation, profile ->
                    local.library.observe(storageKey(activation)).map { result ->
                        when (result) {
                            is StreamCoreResult.Failure -> result
                            is StreamCoreResult.Success -> {
                                val allowedEntries = result.value.filter {
                                    activation.entry.session.services.contentPolicy.isContentAllowed(profile, it.content)
                                }
                                StreamCoreResult.Success(allowedEntries)
                            }
                        }
                    }
                }
            }
            override suspend fun setLiked(profileId: String, content: StreamCoreContent, isLiked: Boolean, changedAtMillis: Long): StreamCoreResult<Unit> {
                return libraryMutation(profileId, content, changedAtMillis, captured) { activation ->
                    local.library.setLiked(storageKey(activation), content, isLiked, changedAtMillis)
                }
            }
            override suspend fun setInMyList(profileId: String, content: StreamCoreContent, isInMyList: Boolean, changedAtMillis: Long): StreamCoreResult<Unit> {
                return libraryMutation(profileId, content, changedAtMillis, captured) { activation ->
                    local.library.setInMyList(storageKey(activation), content, isInMyList, changedAtMillis)
                }
            }
        }
        return RuntimeLibraryService(authorizedEntries, progressForActivation(captured))
    }
    private suspend fun libraryMutation(profileId: String, content: StreamCoreContent, timestamp: Long, captured: CapturedActivation?, block: suspend (CapturedActivation) -> StreamCoreResult<Unit>): StreamCoreResult<Unit> {
        if (!capabilities.localLibrary) return unsupported("library")
        if (content.id.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
        if (timestamp < 0L) return invalid(StreamCoreValidationField.ChangedAtMillis, StreamCoreValidationReason.OutOfRange)
        return profileOperation(profileId, captured) { activation, profile ->
            if (!activation.entry.session.services.contentPolicy.isContentAllowed(profile, content)) {
                failure(StreamCoreError.Unauthorized())
            } else if (!isCurrent(activation)) {
                contextFailure(StreamCoreContextFailureReason.StaleActivation)
            } else {
                block(activation)
            }
        }
    }
    private fun progressForActivation(captured: CapturedActivation?): PlaybackProgressOperations {
        return object : PlaybackProgressOperations {
            override fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
                return this@RuntimeStreamCoreClient.observeProgress(profileId, captured)
            }
            override suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit> {
                return updateProgressForActivation(entry, captured)
            }
            override suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit> {
                return this@RuntimeStreamCoreClient.removeProgress(profileId, contentId, captured)
            }
        }
    }
    private fun observeProgress(profileId: String, captured: CapturedActivation?): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
        return observeLocal(profileId, captured, capabilities.playbackProgress, "playback.progress") { activation, profile ->
            local.progress.observe(storageKey(activation)).map { entries ->
                val allowedEntries = entries.filter {
                    activation.entry.session.services.contentPolicy.isContentAllowed(profile, it.contentSnapshot)
                }
                val profileEntries = allowedEntries.map { it.copy(profileId = profileId) }
                StreamCoreResult.Success(profileEntries)
            }
        }
    }
    private suspend fun getProgress(profileId: String, contentId: String, captured: CapturedActivation?): StreamCoreResult<StreamCorePlaybackProgressEntry?> {
        if (!capabilities.playbackProgress) return unsupported("playback.progress")
        return profileOperation(profileId, captured) { activation, profile ->
            val entry = local.progress.get(storageKey(activation), contentId)
            StreamCoreResult.Success(entry?.takeIf { activation.entry.session.services.contentPolicy.isContentAllowed(profile, it.contentSnapshot) }?.copy(profileId = profileId))
        }
    }
    private suspend fun updateProgressForActivation(entry: StreamCorePlaybackProgressEntry, captured: CapturedActivation?): StreamCoreResult<Unit> {
        if (!capabilities.playbackProgress) return unsupported("playback.progress")
        if (entry.contentId.isBlank()) return invalid(StreamCoreValidationField.ContentId, StreamCoreValidationReason.Required)
        if (entry.contentId != entry.contentSnapshot.id) return invalid(StreamCoreValidationField.ContentSnapshot, StreamCoreValidationReason.Mismatch)
        if (entry.updatedAtMillis < 0L) return invalid(StreamCoreValidationField.ChangedAtMillis, StreamCoreValidationReason.OutOfRange)
        return profileOperation(entry.profileId, captured) { activation, profile ->
            if (!activation.entry.session.services.contentPolicy.isContentAllowed(profile, entry.contentSnapshot)) {
                return@profileOperation failure(StreamCoreError.Unauthorized())
            }
            if (!isCurrent(activation)) {
                return@profileOperation contextFailure(StreamCoreContextFailureReason.StaleActivation)
            }
            if (PlaybackProgressPolicy.isResumable(entry.positionMillis, entry.durationMillis)) {
                local.progress.upsert(entry.copy(profileId = storageKey(activation)))
            } else {
                local.progress.remove(storageKey(activation), entry.contentId)
            }
            StreamCoreResult.Success(Unit)
        }
    }
    private suspend fun removeProgress(profileId: String, contentId: String, captured: CapturedActivation?): StreamCoreResult<Unit> {
        if (!capabilities.playbackProgress) return unsupported("playback.progress")
        return profileOperation(profileId, captured) { activation, _ ->
            local.progress.remove(storageKey(activation), contentId)
            StreamCoreResult.Success(Unit)
        }
    }
    private suspend fun searchForActivation(profileId: String, query: String, captured: CapturedActivation?): StreamCoreResult<List<StreamCoreContent>> {
        if (!capabilities.search) return unsupported("search")
        val normalized = SearchQueryNormalizer.normalize(query)
        return profileOperation(profileId, captured) { activation, profile ->
            if (!SearchQueryNormalizer.isSearchable(normalized)) {
                StreamCoreResult.Success(emptyList())
            } else {
                val searchResult = activation.entry.session.services.search.search(profileId, normalized)
                filterContents(searchResult, activation, profile)
            }
        }
    }
    private suspend fun recordHistory(profileId: String, query: String, captured: CapturedActivation?): StreamCoreResult<Unit> {
        val normalized = SearchQueryNormalizer.normalize(query)
        return historyMutation(profileId, captured) { activation ->
            if (SearchQueryNormalizer.isSearchable(normalized)) {
                local.history.add(storageKey(activation), normalized)
            }
        }
    }
    private suspend fun historyMutation(profileId: String, captured: CapturedActivation?, block: suspend (CapturedActivation) -> Unit): StreamCoreResult<Unit> {
        if (!capabilities.searchHistory) return unsupported("search.history")
        return profileOperation(profileId, captured) { activation, _ ->
            block(activation)
            StreamCoreResult.Success(Unit)
        }
    }
    private suspend fun filterContents(result: StreamCoreResult<List<StreamCoreContent>>, captured: CapturedActivation, profile: StreamCoreProfile): StreamCoreResult<List<StreamCoreContent>> {
        return when (result) {
            is StreamCoreResult.Failure -> result
            is StreamCoreResult.Success -> StreamCoreResult.Success(result.value.filter { captured.entry.session.services.contentPolicy.isContentAllowed(profile, it) })
        }
    }
    private fun <T> observeLocal(profileId: String, captured: CapturedActivation?, supported: Boolean, operation: String, block: suspend (CapturedActivation, StreamCoreProfile) -> Flow<StreamCoreResult<T>>): Flow<StreamCoreResult<T>> {
        if (!supported) return flowOf(unsupported(operation))
        // Old observers never attach to a subsequent authorization activation.
        return state.map { profileFailure(profileId, captured, it) }.distinctUntilChanged().flatMapLatest { error ->
            if (error != null) flowOf(failure(error))
            else flow<StreamCoreResult<T>> {
                val activation = checkNotNull(captured)
                when (val valid = profileOperation(profileId, activation) { _, profile -> StreamCoreResult.Success(profile) }) {
                    is StreamCoreResult.Failure -> emit(valid)
                    is StreamCoreResult.Success -> emitAll(block(activation, valid.value).map { checkedActivationResult(activation, it) })
                }
            }.catch { throwable ->
                if (throwable is CancellationException) throw throwable
                val result = failure((throwable as? ProviderOperationException)?.error ?: StreamCoreError.Storage())
                emit(if (captured != null) checkedActivationResult(captured, result) else result)
            }
        }
    }
    private fun profileIssues(input: StreamCoreCreateProfile, options: StreamCoreProfileEditorOptions): List<StreamCoreValidationIssue> {
        val validation = ProfileValidator.validate(input, options)
        fun reason(error: StreamCoreProfileFieldError): StreamCoreValidationReason {
            return when (error) {
                StreamCoreProfileFieldError.Blank, StreamCoreProfileFieldError.MissingSelection -> StreamCoreValidationReason.Required
                StreamCoreProfileFieldError.TooLong -> StreamCoreValidationReason.TooLong
                StreamCoreProfileFieldError.UnknownSelection -> StreamCoreValidationReason.UnknownSelection
            }
        }
        return buildList {
            validation.displayNameError?.let { add(StreamCoreValidationIssue(StreamCoreValidationField.ProfileName, reason(it))) }
            validation.avatarError?.let { add(StreamCoreValidationIssue(StreamCoreValidationField.AvatarId, reason(it))) }
            validation.parentalLevelError?.let { add(StreamCoreValidationIssue(StreamCoreValidationField.ParentalLevelId, reason(it))) }
        }
    }
    /** Cancels work with this client and maps failures while preserving coroutine cancellation. */
    private suspend fun <T> executeOperation(block: suspend () -> StreamCoreResult<T>): StreamCoreResult<T> {
        if (state.value.closed) {
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
                } catch (_: kotlinx.serialization.SerializationException) {
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

    private fun invalid(field: StreamCoreValidationField, reason: StreamCoreValidationReason): StreamCoreResult.Failure {
        return failure(StreamCoreError.Validation(listOf(StreamCoreValidationIssue(field, reason))))
    }

    private fun contextFailure(reason: StreamCoreContextFailureReason): StreamCoreResult.Failure {
        return failure(StreamCoreError.InvalidContext(reason))
    }

    private fun failure(error: StreamCoreError): StreamCoreResult.Failure {
        return StreamCoreResult.Failure(error)
    }

    private fun unsupported(operation: String): StreamCoreResult.Failure {
        return failure(StreamCoreError.Unsupported(operation))
    }
    private class CapturedSession(val account: StreamCoreAuthAccount, val services: ProviderSessionServices)
    private class EntryAttempt(val session: CapturedSession, val generation: Long)
    private class PinChallenge(val entry: EntryAttempt, val model: StreamCoreProfilePinChallenge)
    private class PinVerification(val challenge: PinChallenge)
    private class CapturedActivation(val id: String, val entry: EntryAttempt, val profile: StreamCoreProfile, val verification: PinVerification?)
    private data class RuntimeState(
        val session: CapturedSession? = null,
        val activation: CapturedActivation? = null,
        val entry: EntryAttempt? = null,
        val challenge: PinChallenge? = null,
        val verification: PinVerification? = null,
        val generation: Long = 0,
        val bootstrapped: Boolean = false,
        val closed: Boolean = false,
    ) {
        fun withoutSelection(): RuntimeState {
            return copy(activation = null, entry = null, challenge = null, verification = null, generation = generation + 1)
        }
    }
}
