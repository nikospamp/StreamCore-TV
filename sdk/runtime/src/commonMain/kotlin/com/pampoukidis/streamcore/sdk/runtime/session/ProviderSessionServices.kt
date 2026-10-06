package com.pampoukidis.streamcore.sdk.runtime.session

import com.pampoukidis.streamcore.sdk.runtime.content.ContentPolicyProvider
import com.pampoukidis.streamcore.sdk.runtime.details.DetailsProvider
import com.pampoukidis.streamcore.sdk.runtime.home.HomeProvider
import com.pampoukidis.streamcore.sdk.runtime.playback.PlaybackProvider
import com.pampoukidis.streamcore.sdk.runtime.profile.ProfileProvider
import com.pampoukidis.streamcore.sdk.runtime.search.SearchProvider

/** Backend adapters for one authenticated account, assembled without network requests. */
data class ProviderSessionServices(
    val profiles: ProfileProvider,
    val home: HomeProvider,
    val details: DetailsProvider,
    val search: SearchProvider,
    val playback: PlaybackProvider,
    val contentPolicy: ContentPolicyProvider,
)
