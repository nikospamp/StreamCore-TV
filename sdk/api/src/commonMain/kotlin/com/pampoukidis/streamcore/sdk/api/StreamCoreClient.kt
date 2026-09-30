package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import kotlinx.coroutines.flow.StateFlow

/**
 * Runtime-enforced services for one owned SDK instance. Factory construction performs no network requests.
 * Bootstrap explicitly, observe [context], then enter a profile through [profiles] before using content/state services.
 * Operation failures use [StreamCoreResult]; coroutine cancellation still propagates as cancellation.
 */
interface StreamCoreClient {
    val configuration: StreamCoreConfiguration
    val capabilities: StreamCoreCapabilities
    /**
     * Authoritative account/authorization state. An initial null account is not a completed bootstrap decision.
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
     * Restores account identity, never a persisted profile authorization. Idempotent after successful bootstrap.
     * Handle a failure before starting dependent work; retry explicitly when appropriate.
     * A fresh client must call [ProfileService.beginEntry] or select a profile before accessing its content.
     */
    suspend fun bootstrap(): StreamCoreResult<StreamCoreContext>
    /**
     * Idempotently releases owned resources and cancels in-flight SDK work without remote logout or data erasure.
     * Cancel host-owned collectors separately. Use [AuthService.logout] when logout is intended.
     */
    fun close()
}
