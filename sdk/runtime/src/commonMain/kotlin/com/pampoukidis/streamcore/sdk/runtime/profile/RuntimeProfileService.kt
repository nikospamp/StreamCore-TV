package com.pampoukidis.streamcore.sdk.runtime.profile

import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcore.sdk.validation.ProfileValidator
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreContextFailureReason
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationIssue
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryChooseProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryNoProfiles
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryPinRequired
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryReady
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileFieldError
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfilePinChallenge
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileSelectionResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.runtime.session.AuthorizedProfile
import com.pampoukidis.streamcore.sdk.runtime.session.PendingProfilePin
import com.pampoukidis.streamcore.sdk.runtime.session.ProfilePinVerification
import com.pampoukidis.streamcore.sdk.runtime.session.ProfileSelectionAttempt
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.session.contextFailure
import com.pampoukidis.streamcore.sdk.runtime.session.failure
import com.pampoukidis.streamcore.sdk.runtime.session.invalid
import com.pampoukidis.streamcore.sdk.runtime.session.unsupported
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkLocalRepositories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

/** Profile management, selection and PIN authorization. All state transitions remain atomic in RuntimeSession. */
internal class RuntimeProfileService(
    private val runtimeSession: RuntimeSession,
    private val capabilities: StreamCoreCapabilities,
    private val local: SdkLocalRepositories,
) : ProfileService {
    private val instanceId = runtimeSession.instanceId

    override suspend fun beginEntry(): StreamCoreResult<StreamCoreProfileEntryResult> {
        val selectionAttemptResult = startProfileSelection()
        if (selectionAttemptResult is StreamCoreResult.Failure) return selectionAttemptResult
        val profileSelection = (selectionAttemptResult as StreamCoreResult.Success).value
        return duringProfileSelection(profileSelection) {
            when (val result = profileSelection.accountSession.providers.profiles.getProfiles()) {
                is StreamCoreResult.Failure -> result
                is StreamCoreResult.Success -> when (result.value.size) {
                    0 -> StreamCoreResult.Success(StreamCoreProfileEntryNoProfiles)
                    1 -> when (val selected = selectAvailableProfile(profileSelection, result.value.single())) {
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
        return runtimeSession.withAuthenticatedAccount { accountSession ->
            val result = accountSession.providers.profiles.getProfiles()
            if (result is StreamCoreResult.Success) runtimeSession.reconcileAvailableProfiles(accountSession, result.value)
            result
        }
    }
    override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> {
        return runtimeSession.withAuthenticatedAccount { it.providers.profiles.getProfileEditorOptions() }
    }
    override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> {
        if (!capabilities.profileCreation) return unsupported("profiles.create")
        return runtimeSession.withAuthenticatedAccount { accountSession ->
            when (val options = accountSession.providers.profiles.getProfileEditorOptions()) {
                is StreamCoreResult.Failure -> options
                is StreamCoreResult.Success -> {
                    val issues = profileIssues(profile, options.value)
                    if (issues.isNotEmpty()) failure(StreamCoreError.Validation(issues))
                    else accountSession.providers.profiles.createProfile(profile.copy(displayName = profile.displayName.trim()))
                }
            }
        }
    }
    override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> {
        if (!capabilities.profileUpdate) return unsupported("profiles.update")
        return runtimeSession.withAccountProfile(profile.profileId) { accountSession, _ ->
            when (val options = accountSession.providers.profiles.getProfileEditorOptions()) {
                is StreamCoreResult.Failure -> options
                is StreamCoreResult.Success -> {
                    val issues = profileIssues(StreamCoreCreateProfile(profile.displayName, profile.avatarId, profile.parentalLevelId), options.value)
                    if (issues.isNotEmpty()) failure(StreamCoreError.Validation(issues))
                    else {
                        val result = accountSession.providers.profiles.updateProfile(profile.copy(displayName = profile.displayName.trim()))
                        if (result is StreamCoreResult.Success) runtimeSession.updateKnownProfile(accountSession, result.value)
                        result
                    }
                }
            }
        }
    }
    override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> {
        if (!capabilities.profileDeletion) return unsupported("profiles.delete")
        return runtimeSession.withAccountProfile(profileId) { accountSession, profile ->
            if (!profile.canDelete) return@withAccountProfile invalid(StreamCoreValidationField.ProfileId, StreamCoreValidationReason.NotAllowed)
            val result = accountSession.providers.profiles.deleteProfile(profileId)
            if (result is StreamCoreResult.Success) runtimeSession.revokeProfile(accountSession, profileId)
            result
        }
    }
    override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfileSelectionResult> {
        if (profileId.isBlank()) return invalid(StreamCoreValidationField.ProfileId, StreamCoreValidationReason.Required)
        val selectionAttemptResult = startProfileSelection()
        if (selectionAttemptResult is StreamCoreResult.Failure) return selectionAttemptResult
        val profileSelection = (selectionAttemptResult as StreamCoreResult.Success).value
        return duringProfileSelection(profileSelection) {
            when (val result = profileSelection.accountSession.providers.profiles.getProfiles()) {
                is StreamCoreResult.Failure -> result
                is StreamCoreResult.Success -> {
                    val profile = result.value.find { it.id == profileId }
                    if (profile == null) contextFailure(StreamCoreContextFailureReason.ProfileUnavailable)
                    else selectAvailableProfile(profileSelection, profile)
                }
            }
        }
    }
    override suspend fun confirmPin(challengeId: String, pin: String): StreamCoreResult<StreamCoreProfile> {
        if (!capabilities.profilePinVerification) {
            return unsupported("profiles.verifyPin")
        }

        // Validate the captured pinChallenge and input before starting this pinVerification attempt.
        val pinChallenge = runtimeSession.state.value.pinChallenge
        if (pinChallenge == null || pinChallenge.model.challengeId != challengeId || !runtimeSession.isCurrent(pinChallenge.profileSelection)) {
            return contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
        }
        if (pin.length != pinChallenge.model.digitCount) {
            return invalid(StreamCoreValidationField.Pin, StreamCoreValidationReason.InvalidLength)
        }
        if (pin.any { it !in '0'..'9' }) {
            return invalid(StreamCoreValidationField.Pin, StreamCoreValidationReason.InvalidFormat)
        }
        val pinVerification = ProfilePinVerification(pinChallenge)
        val verificationState = runtimeSession.state.updateAndGet { current ->
            if (current.pinChallenge === pinChallenge && runtimeSession.isCurrent(pinChallenge.profileSelection, current)) {
                current.copy(pinVerification = pinVerification)
            } else {
                current
            }
        }
        if (verificationState.pinVerification !== pinVerification) {
            return contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
        }

        return try {
            val confirmationResult = runtimeSession.withAuthenticatedAccount(pinChallenge.accountSession) { accountSession ->
                // Recheck the provider's profile and PIN policy before authoritative pinVerification.
                val profilesResult = accountSession.providers.profiles.getProfiles()
                if (profilesResult is StreamCoreResult.Failure) {
                    return@withAuthenticatedAccount profilesResult
                }
                val availableProfiles = (profilesResult as StreamCoreResult.Success).value
                val profile = availableProfiles.find { it.id == pinChallenge.model.profile.id }
                if (profile == null || profile.pinPolicy != pinChallenge.model.profile.pinPolicy) {
                    cancelProfileSelection(pinChallenge.profileSelection)
                    return@withAuthenticatedAccount contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                }
                if (!runtimeSession.isCurrent(pinVerification)) {
                    return@withAuthenticatedAccount contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                }

                val verificationResult = accountSession.providers.profiles.verifyProfilePin(profile.id, pin)
                if (verificationResult is StreamCoreResult.Failure) {
                    return@withAuthenticatedAccount verificationResult
                }
                currentCoroutineContext().ensureActive()
                if (!runtimeSession.isCurrent(pinVerification)) {
                    return@withAuthenticatedAccount contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                }

                // Select only after pinVerification, then persist selection before publishing authorizedProfile.
                val selectionResult = accountSession.providers.profiles.selectProfile(profile.id)
                if (selectionResult is StreamCoreResult.Failure) {
                    return@withAuthenticatedAccount selectionResult
                }
                val selectedProfile = (selectionResult as StreamCoreResult.Success).value
                if (selectedProfile.id != profile.id || selectedProfile.pinPolicy != profile.pinPolicy) {
                    cancelProfileSelection(pinChallenge.profileSelection)
                    return@withAuthenticatedAccount contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
                }
                authorizeSelectedProfile(pinChallenge.profileSelection, selectedProfile, pinVerification)
            }

            // Cancellation/replacement can race with selection or authorizedProfile; reconcile the final result.
            when {
                confirmationResult is StreamCoreResult.Failure &&
                    confirmationResult.error is StreamCoreError.SessionExpired -> confirmationResult
                confirmationResult is StreamCoreResult.Success &&
                    runtimeSession.state.value.authorizedProfile?.pinVerification === pinVerification -> confirmationResult
                runtimeSession.state.value.pinVerification === pinVerification && runtimeSession.isCurrent(pinVerification) -> confirmationResult
                else -> contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
            }
        } finally {
            runtimeSession.state.update { current ->
                if (current.pinVerification === pinVerification) {
                    current.copy(pinVerification = null)
                } else {
                    current
                }
            }
        }
    }
    override fun cancelPin(challengeId: String): StreamCoreResult<Unit> {
        if (runtimeSession.state.value.isClientClosed) return failure(StreamCoreError.Closed())
        val pinChallenge = runtimeSession.state.value.pinChallenge
        if (pinChallenge == null || pinChallenge.model.challengeId != challengeId) return contextFailure(StreamCoreContextFailureReason.PinChallengeExpired)
        runtimeSession.state.update {
            // Confirmation may have committed after the pinChallenge was captured above.
            if (it.pinChallenge === pinChallenge || it.authorizedProfile?.pinVerification?.pinChallenge === pinChallenge) {
                it.withoutProfileAuthorization()
            } else {
                it
            }
        }
        return StreamCoreResult.Success(Unit)
    }
    override suspend fun clearSelection(): StreamCoreResult<Unit> {
        // Revoke memory immediately, before waiting for persistence.
        val previous = runtimeSession.state.getAndUpdate { if (it.isClientClosed) it else it.withoutProfileAuthorization() }
        if (previous.isClientClosed) return failure(StreamCoreError.Closed())
        val accountSession = previous.accountSession ?: return contextFailure(runtimeSession.accountFailure(previous))
        return runtimeSession.withAuthenticatedAccount(accountSession) {
            local.saveSelectedProfile(runtimeSession.configuration, accountSession.account.id, null)
            StreamCoreResult.Success(Unit)
        }
    }

    private fun startProfileSelection(): StreamCoreResult<ProfileSelectionAttempt> {
        val snapshot = runtimeSession.state.value
        if (snapshot.isClientClosed) return failure(StreamCoreError.Closed())
        val accountSession = snapshot.accountSession ?: return contextFailure(runtimeSession.accountFailure(snapshot))
        if (!snapshot.isAuthInitialized) return contextFailure(StreamCoreContextFailureReason.AuthNotInitialized)
        val selectionState = runtimeSession.state.updateAndGet { current ->
            if (current.accountSession === accountSession && !current.isClientClosed) {
                val selectionVersion = current.selectionVersion + 1
                current.copy(authorizedProfile = null, pinChallenge = null, pinVerification = null, selectionVersion = selectionVersion, profileSelection = ProfileSelectionAttempt(accountSession, selectionVersion))
            } else current
        }
        val profileSelection = selectionState.profileSelection
        return if (profileSelection != null && profileSelection.accountSession === accountSession && !selectionState.isClientClosed) StreamCoreResult.Success(profileSelection) else contextFailure(StreamCoreContextFailureReason.StaleSession)
    }

    private suspend fun <T> duringProfileSelection(profileSelection: ProfileSelectionAttempt, block: suspend () -> StreamCoreResult<T>): StreamCoreResult<T> {
        return try {
            val result = runtimeSession.withAuthenticatedAccount(profileSelection.accountSession) {
                if (!runtimeSession.isCurrent(profileSelection)) {
                    contextFailure(StreamCoreContextFailureReason.StaleActivation)
                } else {
                    block()
                }
            }
            if (result is StreamCoreResult.Failure && result.error is StreamCoreError.SessionExpired) {
                result
            } else if (runtimeSession.isCurrent(profileSelection)) {
                result
            } else {
                contextFailure(StreamCoreContextFailureReason.StaleActivation)
            }
        } catch (cancelled: CancellationException) {
            cancelProfileSelection(profileSelection)
            throw cancelled
        }
    }

    private suspend fun selectAvailableProfile(profileSelection: ProfileSelectionAttempt, profile: StreamCoreProfile): StreamCoreResult<StreamCoreProfileSelectionResult> {
        if (!runtimeSession.isCurrent(profileSelection)) return contextFailure(StreamCoreContextFailureReason.StaleActivation)
        if (profile.pinPolicy != null) return requestProfilePin(profileSelection, profile)
        return when (val selected = profileSelection.accountSession.providers.profiles.selectProfile(profile.id)) {
            is StreamCoreResult.Failure -> selected
            is StreamCoreResult.Success -> {
                if (selected.value.id != profile.id) return failure(StreamCoreError.Parsing())
                if (selected.value.pinPolicy != null) return requestProfilePin(profileSelection, selected.value)
                when (val activated = authorizeSelectedProfile(profileSelection, selected.value, null)) {
                    is StreamCoreResult.Failure -> activated
                    is StreamCoreResult.Success -> StreamCoreResult.Success(StreamCoreProfileEntryReady(activated.value))
                }
            }
        }
    }

    private fun requestProfilePin(profileSelection: ProfileSelectionAttempt, profile: StreamCoreProfile): StreamCoreResult<StreamCoreProfileSelectionResult> {
        if (!capabilities.profilePinVerification) return unsupported("profiles.verifyPin")
        val policy = profile.pinPolicy ?: return failure(StreamCoreError.Parsing())
        val pinChallenge = PendingProfilePin(profileSelection, StreamCoreProfilePinChallenge("$instanceId:pin:${profileSelection.selectionVersion}", profile, policy.digitCount))
        val issued = runtimeSession.state.updateAndGet { if (runtimeSession.isCurrent(profileSelection, it)) it.copy(pinChallenge = pinChallenge, pinVerification = null) else it }
        return if (issued.pinChallenge === pinChallenge) StreamCoreResult.Success(StreamCoreProfileEntryPinRequired(pinChallenge.model)) else contextFailure(StreamCoreContextFailureReason.StaleActivation)
    }

    private suspend fun authorizeSelectedProfile(profileSelection: ProfileSelectionAttempt, profile: StreamCoreProfile, pinVerification: ProfilePinVerification?): StreamCoreResult<StreamCoreProfile> {
        if (!runtimeSession.isCurrent(profileSelection) || pinVerification != null && !runtimeSession.isCurrent(pinVerification)) {
            return contextFailure(StreamCoreContextFailureReason.StaleActivation)
        }

        local.saveSelectedProfile(runtimeSession.configuration, profileSelection.accountSession.account.id, profile.id)
        currentCoroutineContext().ensureActive()
        val authorizedProfile = AuthorizedProfile("$instanceId:profile:${profileSelection.selectionVersion}", profileSelection, profile, pinVerification)
        val activated = runtimeSession.state.updateAndGet { current ->
            if (runtimeSession.isCurrent(profileSelection, current) && (pinVerification == null || current.pinVerification === pinVerification)) {
                current.copy(authorizedProfile = authorizedProfile, pinChallenge = null, pinVerification = null)
            } else {
                current
            }
        }
        return if (activated.authorizedProfile === authorizedProfile) {
            StreamCoreResult.Success(profile)
        } else {
            contextFailure(StreamCoreContextFailureReason.StaleActivation)
        }
    }

    private fun cancelProfileSelection(profileSelection: ProfileSelectionAttempt) {
        runtimeSession.state.update { current ->
            if (current.profileSelection === profileSelection) {
                current.withoutProfileAuthorization()
            } else {
                current
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
}
