package com.pampoukidis.streamcoretv.feature.profiles.data

data class EditorRequest(
    val mode: ProfileEditorMode,
    val profileId: String?,
)