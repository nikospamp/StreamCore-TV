package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorMode

data class ProfileEditorScreenUiState(
    val mode: ProfileEditorMode = ProfileEditorMode.Create,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editorOptions: StreamCoreProfileEditorOptions? = null,
    val editor: ProfileEditorFormUiState? = null,
    val profile: StreamCoreProfile? = null,
    val pendingDeleteProfile: StreamCoreProfile? = null,
)
