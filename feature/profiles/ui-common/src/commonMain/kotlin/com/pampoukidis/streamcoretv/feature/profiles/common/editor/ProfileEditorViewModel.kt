package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.api.validation.ProfileValidator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationField
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreValidationReason
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileFieldError
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.EditorRequest
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileEditorViewModel constructor(
    private val profileRepository: ProfileService,
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

            is ProfileEditorAction.KidsProfileChanged -> updateKidsProfile(action.isKids)

            ProfileEditorAction.RequestDeleteProfile -> requestDelete()
            ProfileEditorAction.ConfirmDeleteProfile -> confirmDelete()
            ProfileEditorAction.DismissDeleteConfirmation -> dismissDeleteConfirmation()
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
        if (activeRequest == request) {
            return
        }

        activeRequest = request

        viewModelScope.launch {
            _uiState.value = ProfileEditorScreenUiState(
                mode = mode,
                isLoading = true,
            )

            val options = loadOptions() ?: return@launch
            val profile = when (mode) {
                ProfileEditorMode.Create -> null
                ProfileEditorMode.Edit -> loadEditProfile(profileId) ?: return@launch
            }
            val draft = profile?.toDraftModel() ?: createDraft(options)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    editorOptions = options,
                    profile = profile,
                    editor = ProfileEditorFormUiState(
                        mode = mode,
                        draft = draft,
                    ),
                )
            }
        }
    }

    private suspend fun loadOptions(): StreamCoreProfileEditorOptions? {
        return when (val result = profileRepository.getProfileEditorOptions()) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> {
                _uiState.update { it.copy(isLoading = false) }
                emitError(result.error)
                null
            }
        }
    }

    private fun createDraft(options: StreamCoreProfileEditorOptions): ProfileDraftModel {
        return ProfileDraftModel(
            avatarId = options.avatars.firstOrNull()?.id.orEmpty(),
            parentalLevelId = options.parentalLevels
                .filterNot { it.isKids }
                .maxByOrNull { it.rank }
                ?.id
                .orEmpty(),
        )
    }

    private suspend fun loadEditProfile(profileId: String?): StreamCoreProfile? {
        if (profileId == null) {
            _uiState.update { it.copy(isLoading = false) }
            emitError(StreamCoreError.Unknown())
            emitClose()
            return null
        }

        return when (val result = profileRepository.getProfiles()) {
            is StreamCoreResult.Success -> {
                val profile = result.value.firstOrNull { it.id == profileId }
                if (profile == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    emitError(StreamCoreError.Unknown())
                    emitClose()
                    null
                } else {
                    profile
                }
            }

            is StreamCoreResult.Failure -> {
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
                    validation = ProfileValidator.validate(draft.toCreateInput(), state.editorOptions),
                ),
            )
        }
    }

    private fun updateKidsProfile(isKids: Boolean) {
        val options = _uiState.value.editorOptions ?: return
        val parentalLevel = if (isKids) {
            options.parentalLevels.firstOrNull { it.isKids }
        } else {
            options.parentalLevels
                .filterNot { it.isKids }
                .maxByOrNull { it.rank }
        } ?: return

        updateDraft {
            it.copy(parentalLevelId = parentalLevel.id)
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isSaving) return

        val editor = state.editor ?: return
        val validation = ProfileValidator.validate(editor.draft.toCreateInput(), state.editorOptions)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(editor = editor.copy(validation = validation))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = when (editor.mode) {
                ProfileEditorMode.Create -> profileRepository.createProfile(editor.draft.toCreateInput())
                ProfileEditorMode.Edit -> profileRepository.updateProfile(
                    StreamCoreUpdateProfile(
                        profileId = editor.draft.profileId.orEmpty(),
                        displayName = editor.draft.displayName,
                        avatarId = editor.draft.avatarId,
                        parentalLevelId = editor.draft.parentalLevelId,
                    ),
                )
            }
            handleSubmitResult(result)
        }
    }

    private fun requestDelete() {
        val state = _uiState.value
        val profile = state.profile ?: return
        if (!profile.canDelete || state.isSaving) return

        _uiState.update { it.copy(pendingDeleteProfile = profile) }
    }

    private fun dismissDeleteConfirmation() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(pendingDeleteProfile = null) }
    }

    private fun confirmDelete() {
        val profile = _uiState.value.pendingDeleteProfile ?: return
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = profileRepository.deleteProfile(profile.id)) {
                is StreamCoreResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            pendingDeleteProfile = null,
                        )
                    }
                    effectsChannel.send(ProfileEditorEffect.ProfileDeleted)
                }

                is StreamCoreResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            pendingDeleteProfile = null,
                        )
                    }
                    emitError(result.error)
                }
            }
        }
    }

    private suspend fun handleSubmitResult(result: StreamCoreResult<StreamCoreProfile>) {
        when (result) {
            is StreamCoreResult.Success -> {
                _uiState.update { it.copy(isSaving = false) }
                effectsChannel.send(ProfileEditorEffect.ProfileSaved)
            }

            is StreamCoreResult.Failure -> {
                _uiState.update { it.copy(isSaving = false) }
                if (!applyOperationValidation(result.error)) emitError(result.error)
            }
        }
    }

    private fun applyOperationValidation(error: StreamCoreError): Boolean {
        if (error !is StreamCoreError.Validation || error.issues.isEmpty()) return false
        val state = _uiState.value
        val editor = state.editor ?: return false
        var validation = editor.validation
        var mapped = 0
        error.issues.forEach { issue ->
            val fieldError = when (issue.reason) {
                StreamCoreValidationReason.Required -> if (issue.field == StreamCoreValidationField.ProfileName) StreamCoreProfileFieldError.Blank else StreamCoreProfileFieldError.MissingSelection
                StreamCoreValidationReason.TooLong -> if (issue.field == StreamCoreValidationField.ProfileName) StreamCoreProfileFieldError.TooLong else null
                StreamCoreValidationReason.UnknownSelection -> StreamCoreProfileFieldError.UnknownSelection
                else -> null
            }
            if (fieldError != null) {
                when (issue.field) {
                    StreamCoreValidationField.ProfileName -> { validation = validation.copy(displayNameError = fieldError); mapped++ }
                    StreamCoreValidationField.AvatarId -> { validation = validation.copy(avatarError = fieldError); mapped++ }
                    StreamCoreValidationField.ParentalLevelId -> { validation = validation.copy(parentalLevelError = fieldError); mapped++ }
                    else -> Unit
                }
            }
        }
        _uiState.update { it.copy(editor = editor.copy(validation = validation)) }
        return mapped == error.issues.size
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

    private suspend fun emitError(error: StreamCoreError) {
        effectsChannel.send(ProfileEditorEffect.ShowError(error))
    }

    private fun ProfileDraftModel.toCreateInput(): StreamCoreCreateProfile {
        return StreamCoreCreateProfile(displayName, avatarId, parentalLevelId)
    }

    private fun StreamCoreProfile.toDraftModel(): ProfileDraftModel {
        return ProfileDraftModel(
            profileId = id,
            displayName = displayName,
            avatarId = avatar.id,
            parentalLevelId = parentalLevel.id,
        )
    }
}
