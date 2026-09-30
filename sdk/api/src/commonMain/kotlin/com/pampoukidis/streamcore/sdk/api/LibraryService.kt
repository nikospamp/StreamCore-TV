package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreContentLibraryState
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibrary
import kotlinx.coroutines.flow.Flow

/**
 * Saved membership and derived collections for the active authorized profile; IDs remain explicit and must match it.
 * Observers capture the activation when created. After activation changes, call the observation method again
 * and collect its new flow; recollecting an old flow does not bind it to the new authorization.
 */
interface LibraryService {
    /** Combines liked/my-list membership with resumable progress; failures are emitted as [StreamCoreResult]. */
    fun observe(profileId: String): Flow<StreamCoreResult<StreamCoreLibrary>>
    /** Observes liked/my-list membership for one content ID within the captured activation. */
    fun observeContentState(profileId: String, contentId: String): Flow<StreamCoreResult<StreamCoreContentLibraryState>>
    /** Applies content policy and SDK-owned membership timestamps. */
    suspend fun setLiked(profileId: String, content: StreamCoreContent, isLiked: Boolean): StreamCoreResult<Unit>
    /** Applies content policy and SDK-owned membership timestamps independently of liked state. */
    suspend fun setInMyList(profileId: String, content: StreamCoreContent, isInMyList: Boolean): StreamCoreResult<Unit>
}
