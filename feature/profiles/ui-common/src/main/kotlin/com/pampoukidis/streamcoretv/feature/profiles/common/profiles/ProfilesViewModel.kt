package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.profiles.domain.DeleteProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.LoadProfilesUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.SelectProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val loadProfiles: LoadProfilesUseCase,
    private val deleteProfile: DeleteProfileUseCase,
    private val selectProfile: SelectProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<ProfilesEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<ProfilesEffect> = effectsChannel.receiveAsFlow()

    init {
        refresh()
    }

    fun onAction(action: ProfilesAction) {
        when (action) {
            ProfilesAction.Refresh -> refresh()
            ProfilesAction.ManageProfiles -> enterManageMode()
            ProfilesAction.DoneManaging -> exitManageMode()
            is ProfilesAction.SelectProfile -> select(action.profileId)
            is ProfilesAction.RequestDeleteProfile -> requestDelete(action.profileId)
            ProfilesAction.ConfirmDeleteProfile -> confirmDelete()
            ProfilesAction.DismissDeleteConfirmation -> _uiState.update { it.copy(pendingDeleteProfile = null) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadError = null,
                )
            }
            when (val result = loadProfiles()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        profiles = result.value,
                        loadError = null,
                    )
                }

                is AppResult.Failure -> {
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
        if (state.isLoading || state.isSaving || state.pendingSelectionProfileId != null) return
        if (state.profiles.isEmpty()) return
        _uiState.update { it.copy(mode = ProfilesMode.Manage) }
    }

    private fun exitManageMode() {
        _uiState.update { it.copy(mode = ProfilesMode.Selection) }
    }

    private fun select(profileId: String) {
        val state = _uiState.value
        if (state.mode != ProfilesMode.Selection) return
        if (state.isLoading || state.isSaving || state.pendingSelectionProfileId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(pendingSelectionProfileId = profileId) }
            when (val result = selectProfile(profileId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(pendingSelectionProfileId = null) }
                    effectsChannel.send(ProfilesEffect.ProfileSelected(result.value))
                }

                is AppResult.Failure -> {
                    _uiState.update { it.copy(pendingSelectionProfileId = null) }
                    emitError(result.error)
                }
            }
        }
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
            when (val result = deleteProfile(profile.id)) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        pendingDeleteProfile = null,
                        profiles = state.profiles.filterNot { it.id == profile.id },
                    )
                }

                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }
                    emitError(result.error)
                }
            }
        }
    }

    private suspend fun emitError(error: AppError) {
        effectsChannel.send(ProfilesEffect.ShowError(error))
    }
}