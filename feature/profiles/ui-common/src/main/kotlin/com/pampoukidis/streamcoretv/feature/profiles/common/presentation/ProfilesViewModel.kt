package com.pampoukidis.streamcoretv.feature.profiles.common.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfilesEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.domain.CreateProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.DeleteProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.LoadProfileEditorOptionsUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.LoadProfilesUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.SelectProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.UpdateProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.ValidateProfileDraftUseCase
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
    private val loadProfileEditorOptions: LoadProfileEditorOptionsUseCase,
    private val validateProfileDraft: ValidateProfileDraftUseCase,
    private val createProfile: CreateProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
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
            is ProfilesAction.SelectProfile -> select(action.profileId)
            ProfilesAction.StartCreateProfile -> startCreateProfile()
            is ProfilesAction.StartEditProfile -> startEditProfile(action.profileId)
            is ProfilesAction.DisplayNameChanged -> updateDraft { it.copy(displayName = action.value) }
            is ProfilesAction.AvatarChanged -> updateDraft { it.copy(avatarId = action.avatarId) }
            is ProfilesAction.ParentalLevelChanged -> updateDraft {
                it.copy(parentalLevelId = action.parentalLevelId)
            }
            ProfilesAction.SubmitEditor -> submitEditor()
            ProfilesAction.DismissEditor -> _uiState.update { it.copy(editor = null) }
            is ProfilesAction.RequestDeleteProfile -> requestDelete(action.profileId)
            ProfilesAction.ConfirmDeleteProfile -> confirmDelete()
            ProfilesAction.DismissDeleteConfirmation -> _uiState.update { it.copy(pendingDeleteProfile = null) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = loadProfiles()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        profiles = result.value,
                    )
                }

                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    emitError(result.error)
                }
            }
        }
    }

    private fun select(profileId: String) {
        if (_uiState.value.pendingSelectionProfileId != null) return

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

    private fun startCreateProfile() {
        viewModelScope.launch {
            val options = ensureEditorOptions() ?: return@launch
            _uiState.update {
                it.copy(
                    editor = ProfileEditorUiState(
                        mode = ProfileEditorMode.Create,
                        draft = ProfileDraftModel(
                            avatarId = options.avatars.firstOrNull()?.id.orEmpty(),
                            parentalLevelId = options.parentalLevels.firstOrNull()?.id.orEmpty(),
                        ),
                    ),
                )
            }
        }
    }

    private fun startEditProfile(profileId: String) {
        val profile = _uiState.value.profiles.firstOrNull { it.id == profileId } ?: return

        viewModelScope.launch {
            ensureEditorOptions() ?: return@launch
            _uiState.update {
                it.copy(
                    editor = ProfileEditorUiState(
                        mode = ProfileEditorMode.Edit,
                        draft = profile.toDraftModel(),
                    ),
                )
            }
        }
    }

    private suspend fun ensureEditorOptions() =
        _uiState.value.editorOptions ?: when (val result = loadProfileEditorOptions()) {
            is AppResult.Success -> {
                _uiState.update { it.copy(editorOptions = result.value) }
                result.value
            }

            is AppResult.Failure -> {
                emitError(result.error)
                null
            }
        }

    private fun updateDraft(transform: (ProfileDraftModel) -> ProfileDraftModel) {
        _uiState.update { state ->
            val editor = state.editor ?: return@update state
            val draft = transform(editor.draft)
            state.copy(
                editor = editor.copy(
                    draft = draft,
                    validation = validateProfileDraft(draft, state.editorOptions),
                ),
            )
        }
    }

    private fun submitEditor() {
        val editor = _uiState.value.editor ?: return
        val validation = validateProfileDraft(editor.draft, _uiState.value.editorOptions)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(editor = editor.copy(validation = validation))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = when (editor.mode) {
                ProfileEditorMode.Create -> createProfile(editor.draft)
                ProfileEditorMode.Edit -> updateProfile(editor.draft)
            }
            handleEditorResult(result)
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

    private suspend fun handleEditorResult(result: AppResult<ProfileModel>) {
        when (result) {
            is AppResult.Success -> _uiState.update { state ->
                val updatedProfile = result.value
                val profiles = if (state.profiles.any { it.id == updatedProfile.id }) {
                    state.profiles.map { profile ->
                        if (profile.id == updatedProfile.id) updatedProfile else profile
                    }
                } else {
                    state.profiles + updatedProfile
                }

                state.copy(
                    isSaving = false,
                    profiles = profiles,
                    editor = null,
                )
            }

            is AppResult.Failure -> {
                _uiState.update { it.copy(isSaving = false) }
                emitError(result.error)
            }
        }
    }

    private suspend fun emitError(error: AppError) {
        effectsChannel.send(ProfilesEffect.ShowError(error))
    }

    private fun ProfileModel.toDraftModel() = ProfileDraftModel(
        profileId = id,
        displayName = displayName,
        avatarId = avatar.id,
        parentalLevelId = parentalLevel.id,
    )
}
