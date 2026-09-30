package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

sealed interface ProfileEditorEffect {
    data object ProfileSaved : ProfileEditorEffect
    data object ProfileDeleted : ProfileEditorEffect
    data object Close : ProfileEditorEffect
    data class ShowError(val error: StreamCoreError) : ProfileEditorEffect
}


