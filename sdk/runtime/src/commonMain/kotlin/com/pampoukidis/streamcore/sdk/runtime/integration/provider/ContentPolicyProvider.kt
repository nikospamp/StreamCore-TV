package com.pampoukidis.streamcore.sdk.runtime.integration.provider

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile

/**
 * Backend interpretation of content restrictions. Runtime invokes this policy for catalogue results,
 * direct details/playback requests, and saved library/progress content. Numeric ratings are not universal policy.
 * Throw [ProviderOperationException] for a typed backend failure; coroutine cancellation propagates.
 */
interface ContentPolicyProvider {
    suspend fun isContentAllowed(profile: StreamCoreProfile, content: StreamCoreContent): Boolean
}
