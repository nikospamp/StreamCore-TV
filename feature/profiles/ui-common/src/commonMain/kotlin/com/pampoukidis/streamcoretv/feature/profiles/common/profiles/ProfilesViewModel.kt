package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import com.pampoukidis.streamcore.sdk.api.ProfileService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.profile.*
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinFailure
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfilesViewModel constructor(
    private val profileRepository: ProfileService,
    autoEnterSingleProfile: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<ProfilesEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<ProfilesEffect> = effectsChannel.receiveAsFlow()

    private var entryPending = autoEnterSingleProfile
    private var hasEnteredRoute = false
    private var loadJob: Job? = null
    private var selectionJob: Job? = null
    private var selectionRevision = 0
    private var pinChallenge: StreamCoreProfilePinChallenge? = null

    init {
        if (autoEnterSingleProfile) effectsChannel.trySend(ProfilesEffect.EntryStarted)
        refresh()
    }

    fun onAction(action: ProfilesAction) {
        when (action) {
            ProfilesAction.RouteEntered -> {
                if (hasEnteredRoute) refresh() else hasEnteredRoute = true
            }
            ProfilesAction.Refresh -> refresh()
            ProfilesAction.ManageProfiles -> enterManageMode()
            ProfilesAction.DoneManaging -> exitManageMode()
            is ProfilesAction.SelectProfile -> select(action.profileId)
            is ProfilesAction.RequestDeleteProfile -> requestDelete(action.profileId)
            ProfilesAction.ConfirmDeleteProfile -> confirmDelete()
            ProfilesAction.DismissDeleteConfirmation -> _uiState.update { it.copy(pendingDeleteProfile = null) }
            is ProfilesAction.PinDraftChanged -> changePinDraft(action.value)
            is ProfilesAction.PinDigitEntered -> enterPinDigit(action.digit)
            ProfilesAction.PinDeleteDigit -> deletePinDigit()
            ProfilesAction.RetryPin -> submitPin()
            ProfilesAction.CancelPin -> cancelPin()
        }
    }

    private fun refresh() {
        if (loadJob?.isActive == true || _uiState.value.pin != null) return
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadError = null,
                )
            }
            if (entryPending) {
                when (val result = profileRepository.beginEntry()) {
                    is StreamCoreResult.Success -> {
                        entryPending = false
                        _uiState.update { it.copy(isLoading = false, loadError = null) }
                        when (val entry = result.value) {
                            StreamCoreProfileEntryNoProfiles -> _uiState.update { it.copy(profiles = emptyList()) }
                            is StreamCoreProfileEntryChooseProfile -> _uiState.update { it.copy(profiles = entry.profiles) }
                            is StreamCoreProfileEntryPinRequired -> {
                                _uiState.update { it.copy(profiles = listOf(entry.challenge.profile)) }
                                showPin(entry.challenge)
                            }
                            is StreamCoreProfileEntryReady -> {
                                _uiState.update { it.copy(profiles = listOf(entry.profile)) }
                                profileSelected(entry.profile)
                            }
                        }
                    }
                    is StreamCoreResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false, loadError = result.error) }
                        emitError(result.error)
                    }
                }
                return@launch
            }
            when (val result = profileRepository.getProfiles()) {
                is StreamCoreResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        profiles = result.value,
                        loadError = null,
                    )
                }

                is StreamCoreResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = result.error,
                        )
                    }
                    emitError(result.error)
                }
            }
        }
    }

    private fun enterManageMode() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.pendingSelectionProfileId != null || state.pin != null) return
        if (state.profiles.isEmpty()) return
        _uiState.update { it.copy(mode = ProfilesMode.Manage) }
    }

    private fun exitManageMode() {
        _uiState.update { it.copy(mode = ProfilesMode.Selection) }
    }

    private fun select(profileId: String) {
        val state = _uiState.value
        if (state.mode != ProfilesMode.Selection) return
        if (state.isLoading || state.isSaving || state.pendingSelectionProfileId != null || state.pin != null) return

        val revision = ++selectionRevision
        _uiState.update { it.copy(pendingSelectionProfileId = profileId, restoreFocusProfileId = null) }
        selectionJob = viewModelScope.launch {
            when (val result = profileRepository.selectProfile(profileId)) {
                is StreamCoreResult.Success -> {
                    if (revision != selectionRevision) return@launch
                    when (val selection = result.value) {
                        is StreamCoreProfileEntryReady -> profileSelected(selection.profile)
                        is StreamCoreProfileEntryPinRequired -> showPin(selection.challenge)
                    }
                }

                is StreamCoreResult.Failure -> {
                    if (revision != selectionRevision) return@launch
                    _uiState.update { it.copy(pendingSelectionProfileId = null) }
                    emitError(result.error)
                }
            }
        }
    }

    private fun showPin(challenge: StreamCoreProfilePinChallenge) {
        pinChallenge = challenge
        _uiState.update {
            it.copy(
                pendingSelectionProfileId = null,
                pin = ProfilePinUiState(challenge.profile, challenge.digitCount, challengeId = challenge.challengeId),
                restoreFocusProfileId = challenge.profile.id,
            )
        }
    }

    private fun enterPinDigit(digit: Int) {
        if (digit !in 0..9) return
        val pin = _uiState.value.pin ?: return
        changePinDraft(pin.draft + digit)
    }

    private fun deletePinDigit() {
        val pin = _uiState.value.pin ?: return
        changePinDraft(pin.draft.dropLast(1))
    }

    private fun changePinDraft(value: String) {
        val pin = _uiState.value.pin ?: return
        if (!pin.inputEnabled) return
        val normalized = value.filter { it in '0'..'9' }.take(pin.digitCount)
        if (normalized == pin.draft) return
        _uiState.update { it.copy(pin = pin.copy(draft = normalized, failure = null)) }
        if (normalized.length == pin.digitCount) submitPin()
    }

    private fun submitPin() {
        val challenge = pinChallenge ?: return
        val pin = _uiState.value.pin ?: return
        if (!pin.inputEnabled || pin.draft.length != pin.digitCount || selectionJob?.isActive == true) return
        val revision = ++selectionRevision
        _uiState.update { it.copy(pin = pin.copy(isSubmitting = true, failure = null)) }
        selectionJob = viewModelScope.launch {
            try {
                when (val result = profileRepository.confirmPin(challenge.challengeId, pin.draft)) {
                    is StreamCoreResult.Success -> if (revision == selectionRevision) profileSelected(result.value)
                    is StreamCoreResult.Failure -> {
                        if (revision != selectionRevision) return@launch
                        when (val error = result.error) {
                            is StreamCoreError.PinRejected -> _uiState.update {
                                it.copy(pin = pin.copy(
                                    draft = "",
                                    failure = if (error.remainingAttempts == 0) ProfilePinFailure.Locked else ProfilePinFailure.Incorrect,
                                    inputRevision = pin.inputRevision + 1,
                                ))
                            }
                            is StreamCoreError.Network, is StreamCoreError.Timeout, is StreamCoreError.Server -> _uiState.update {
                                it.copy(pin = pin.copy(failure = ProfilePinFailure.Unavailable))
                            }
                            is StreamCoreError.Validation -> _uiState.update {
                                it.copy(pin = pin.copy(draft = "", failure = ProfilePinFailure.Incorrect, inputRevision = pin.inputRevision + 1))
                            }
                            else -> {
                                ++selectionRevision
                                pinChallenge = null
                                profileRepository.cancelPin(challenge.challengeId)
                                _uiState.update { it.copy(pin = null, pendingSelectionProfileId = null, restoreFocusProfileId = pin.profile.id) }
                                emitError(error)
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                if (revision == selectionRevision) {
                    pinChallenge = null
                    profileRepository.cancelPin(challenge.challengeId)
                    _uiState.update { it.copy(pin = null, pendingSelectionProfileId = null, restoreFocusProfileId = pin.profile.id) }
                }
                throw cancelled
            }
        }
    }

    private fun cancelPin() {
        val challenge = pinChallenge ?: return
        ++selectionRevision
        pinChallenge = null
        profileRepository.cancelPin(challenge.challengeId)
        selectionJob?.cancel()
        selectionJob = null
        _uiState.update { it.copy(pin = null, pendingSelectionProfileId = null, restoreFocusProfileId = challenge.profile.id) }
    }

    private suspend fun profileSelected(profile: StreamCoreProfile) {
        pinChallenge = null
        _uiState.update { it.copy(pin = null, pendingSelectionProfileId = null) }
        effectsChannel.send(ProfilesEffect.ProfileSelected(profile))
    }

    override fun onCleared() {
        pinChallenge?.let { profileRepository.cancelPin(it.challengeId) }
        pinChallenge = null
        _uiState.update { it.copy(pin = null) }
        super.onCleared()
    }

    private fun requestDelete(profileId: String) {
        val profile = _uiState.value.profiles.firstOrNull { it.id == profileId } ?: return
        if (!profile.canDelete) return
        _uiState.update { it.copy(pendingDeleteProfile = profile) }
    }

    private fun confirmDelete() {
        val profile = _uiState.value.pendingDeleteProfile ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = profileRepository.deleteProfile(profile.id)) {
                is StreamCoreResult.Success -> _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        pendingDeleteProfile = null,
                        profiles = state.profiles.filterNot { it.id == profile.id },
                    )
                }

                is StreamCoreResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }
                    emitError(result.error)
                }
            }
        }
    }

    private suspend fun emitError(error: StreamCoreError) {
        effectsChannel.send(ProfilesEffect.ShowError(error))
    }
}
