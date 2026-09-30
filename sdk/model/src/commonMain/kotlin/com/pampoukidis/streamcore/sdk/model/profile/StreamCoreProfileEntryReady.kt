package com.pampoukidis.streamcore.sdk.model.profile

/**
 * Already activated in client context. Navigate to content without selecting the profile a second time.
 */
data class StreamCoreProfileEntryReady(val profile: StreamCoreProfile) : StreamCoreProfileEntryResult, StreamCoreProfileSelectionResult
