package com.pampoukidis.streamcoretv.feature.profiles.common.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorEffect
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorUiState
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.domain.CreateProfileUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.LoadProfileEditorOptionsUseCase
import com.pampoukidis.streamcoretv.feature.profiles.domain.LoadProfilesUseCase
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
class ProfileEditorViewModel @Inject constructor(
    private val loadProfiles: LoadProfilesUseCase,
    private val loadProfileEditorOptions: LoadProfileEditorOptionsUseCase,
    private val validateProfileDraft: ValidateProfileDraftUseCase,
    private val createProfile: CreateProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditorScreenUiState())
    val uiState: StateFlow<ProfileEditorScreenUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<ProfileEditorEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<ProfileEditorEffect> = effectsChannel.receiveAsFlow()

    private var activeRequest: EditorRequest? = null

    fun onAction(action: ProfileEditorAction) {
        when (action) {
            is ProfileEditorAction.Load -> loadEditor(
                mode = action.mode,
                profileId = action.profileId,
            )

            is ProfileEditorAction.DisplayNameChanged -> updateDraft {
                it.copy(displayName = action.value)
            }

            is ProfileEditorAction.AvatarChanged -> updateDraft {
                it.copy(avatarId = action.avatarId)
            }

            is ProfileEditorAction.ParentalLevelChanged -> updateDraft {
                it.copy(parentalLevelId = action.parentalLevelId)
            }

            ProfileEditorAction.Submit -> submit()
            ProfileEditorAction.Cancel -> close()
        }
    }

    private fun loadEditor(
        mode: ProfileEditorMode,
        profileId: String?,
    ) {
        val request = EditorRequest(
            mode = mode,
            profileId = profileId,
        )
        if (activeRequest == request) return
        activeRequest = request

        viewModelScope.launch {
            _uiState.value = ProfileEditorScreenUiState(
                mode = mode,
                isLoading = true,
            )

            val options = loadOptions() ?: return@launch
            val draft = when (mode) {
                ProfileEditorMode.Create -> createDraft(options)
                ProfileEditorMode.Edit -> loadEditDraft(profileId)
            } ?: return@launch

            _uiState.update {
                it.copy(
                    isLoading = false,
                    editorOptions = options,
                    editor = ProfileEditorUiState(
                        mode = mode,
                        draft = draft,
                    ),
                )
            }
        }
    }

    private suspend fun loadOptions(): ProfileEditorOptionsModel? {
        return when (val result = loadProfileEditorOptions()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> {
                _uiState.update { it.copy(isLoading = false) }
                emitError(result.error)
                null
            }
        }
    }

    private fun createDraft(options: ProfileEditorOptionsModel): ProfileDraftModel {
        return ProfileDraftModel(
            avatarId = options.avatars.firstOrNull()?.id.orEmpty(),
            parentalLevelId = options.parentalLevels.firstOrNull()?.id.orEmpty(),
        )
    }

    private suspend fun loadEditDraft(profileId: String?): ProfileDraftModel? {
        if (profileId == null) {
            _uiState.update { it.copy(isLoading = false) }
            emitError(AppError.Unknown())
            emitClose()
            return null
        }

        return when (val result = loadProfiles()) {
            is AppResult.Success -> {
                val profile = result.value.firstOrNull { it.id == profileId }
                if (profile == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    emitError(AppError.Unknown())
                    emitClose()
                    null
                } else {
                    profile.toDraftModel()
                }
            }

            is AppResult.Failure -> {
                _uiState.update { it.copy(isLoading = false) }
                emitError(result.error)
                null
            }
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

    private fun submit() {
        val state = _uiState.value
        if (state.isSaving) return

        val editor = state.editor ?: return
        val validation = validateProfileDraft(editor.draft, state.editorOptions)
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
            handleSubmitResult(result)
        }
    }

    private suspend fun handleSubmitResult(result: AppResult<ProfileModel>) {
        when (result) {
            is AppResult.Success -> {
                _uiState.update { it.copy(isSaving = false) }
                effectsChannel.send(ProfileEditorEffect.ProfileSaved)
            }

            is AppResult.Failure -> {
                _uiState.update { it.copy(isSaving = false) }
                emitError(result.error)
            }
        }
    }

    private fun close() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            emitClose()
        }
    }

    private suspend fun emitClose() {
        effectsChannel.send(ProfileEditorEffect.Close)
    }

    private suspend fun emitError(error: AppError) {
        effectsChannel.send(ProfileEditorEffect.ShowError(error))
    }

    private fun ProfileModel.toDraftModel(): ProfileDraftModel {
        return ProfileDraftModel(
            profileId = id,
            displayName = displayName,
            avatarId = avatar.id,
            parentalLevelId = parentalLevel.id,
        )
    }

    private data class EditorRequest(
        val mode: ProfileEditorMode,
        val profileId: String?,
    )
}
