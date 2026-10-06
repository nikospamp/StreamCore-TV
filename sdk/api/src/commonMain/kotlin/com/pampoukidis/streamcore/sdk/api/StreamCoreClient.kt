package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import kotlinx.coroutines.flow.StateFlow

/**
 * Runtime-enforced services for one owned SDK instance. Factory construction performs no network requests.
 * Restore through [AuthService.restoreSession] or log in, observe [context], then enter a profile through [profiles].
 * Operation failures use [StreamCoreResult]; coroutine cancellation still propagates as cancellation.
 */
interface StreamCoreClient {
    val configuration: StreamCoreConfiguration
    val capabilities: StreamCoreCapabilities

    /**
     * Authoritative account/authorization state. An initial null account is not a completed authentication decision.
     * The host owns its collector's scope; [close] does not complete this flow or cancel host collectors.
     */
    val context: StateFlow<StreamCoreContext>
    val auth: AuthService
    val profiles: ProfileService
    val home: HomeService
    val details: DetailsService
    val search: SearchService
    val library: LibraryService
    val playback: PlaybackService

    /**
     * Idempotently releases owned resources and cancels in-flight SDK work without remote logout or data erasure.
     * Cancel host-owned collectors separately. Use [AuthService.logout] when logout is intended.
     */
    fun close()
}
