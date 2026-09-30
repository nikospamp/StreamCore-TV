package com.pampoukidis.streamcore.sdk.model

import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackSupport

data class StreamCoreCapabilities(
    val credentialsLogin: Boolean = true,
    val qrLogin: Boolean = false,
    val passwordRecovery: Boolean = false,
    val profileCreation: Boolean = true,
    val profileUpdate: Boolean = true,
    val profileDeletion: Boolean = true,
    val profilePinVerification: Boolean = false,
    val search: Boolean = true,
    val localLibrary: Boolean = true,
    val searchHistory: Boolean = true,
    val playbackProgress: Boolean = true,
    val playback: StreamCorePlaybackSupport = StreamCorePlaybackSupport.Unsupported,
)
