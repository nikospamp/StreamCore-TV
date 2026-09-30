package com.pampoukidis.streamcore.sdk.model.profile

data class StreamCoreProfileEditorOptions(
    val avatars: List<StreamCoreProfileAvatar>,
    val parentalLevels: List<StreamCoreProfileParentalLevel>,
)
