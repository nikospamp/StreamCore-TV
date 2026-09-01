package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode

data class ProfileEditorScreenUiState(
    val mode: ProfileEditorMode = ProfileEditorMode.Create,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editorOptions: ProfileEditorOptionsModel? = null,
    val editor: ProfileEditorFormUiState? = null,
    val profile: ProfileModel? = null,
    val pendingDeleteProfile: ProfileModel? = null,
)