package com.pampoukidis.streamcoretv.feature.profiles.common.editor

data class EditorRequest(
    val mode: ProfileEditorMode,
    val profileId: String?,
)
