package com.pampoukidis.streamcore.sdk.runtime.integration.provider

/** Backend adapters for one authenticated account, assembled without network requests. */
data class ProviderSessionServices(
    val profiles: ProfileProvider,
    val home: HomeProvider,
    val details: DetailsProvider,
    val search: SearchProvider,
    val playback: PlaybackProvider,
    val contentPolicy: ContentPolicyProvider,
)
