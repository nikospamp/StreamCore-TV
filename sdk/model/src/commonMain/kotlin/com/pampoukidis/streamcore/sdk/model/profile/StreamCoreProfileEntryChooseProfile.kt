package com.pampoukidis.streamcore.sdk.model.profile

/**
 * Multiple owned profiles require a user's choice. No authorization was granted by this outcome.
 */
data class StreamCoreProfileEntryChooseProfile(val profiles: List<StreamCoreProfile>) : StreamCoreProfileEntryResult
