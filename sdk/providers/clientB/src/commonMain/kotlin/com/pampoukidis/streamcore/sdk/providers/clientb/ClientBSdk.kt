package com.pampoukidis.streamcore.sdk.providers.clientb

import com.pampoukidis.streamcoretv.client.clientb.data.auth.*
import com.pampoukidis.streamcoretv.client.clientb.data.catalog.*
import com.pampoukidis.streamcoretv.client.clientb.data.profile.ClientBProfileRepository
import com.pampoukidis.streamcoretv.client.clientb.player.ClientBPlaybackSourceRepository
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackSupport
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.runtime.RuntimeStreamCoreClient
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ContentPolicyProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderSessionFactory
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderSessionServices
import com.pampoukidis.streamcore.sdk.runtime.storage.PreferencesSdkStorage
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkPlatformStorage
import kotlinx.serialization.json.Json

object ClientBSdk {
    fun createInMemory(config: ClientBSdkConfiguration): StreamCoreClient {
        return createClientBSdk(config.copy(common = config.common.copy(persistence = StreamCorePersistenceMode.InMemory)), SdkPlatformStorage.inMemory())
    }
}

internal fun createClientBSdk(config: ClientBSdkConfiguration, storage: SdkPlatformStorage): StreamCoreClient {
    val catalogueSource = ClientBCatalogSource()
    val accountProfiles = mutableMapOf<String, ClientBProfileRepository>()
    return RuntimeStreamCoreClient(
        configuration = config.common,
        capabilities = StreamCoreCapabilities(
            playback = if (config.demoPlayback) StreamCorePlaybackSupport.DemoMedia else StreamCorePlaybackSupport.Unsupported,
            profilePinVerification = true,
        ),
        authentication = ClientBAuthenticateRepository(ClientBPreferencesAuthStore(storage.auth)),
        sessions = ProviderSessionFactory { account ->
            val profiles = accountProfiles.getOrPut(account.id) { ClientBProfileRepository(config.referenceProfileScenario) }
            val home = ClientBCatalogRepository(catalogueSource)
            val details = ClientBDetailsRepository(catalogueSource)
            val search = ClientBSearchRepository(catalogueSource, profiles)
            val contentPolicy = object : ContentPolicyProvider {
                override suspend fun isContentAllowed(profile: StreamCoreProfile, content: StreamCoreContent): Boolean {
                    return catalogueSource.contentForProfile(profile.isKidsProfile).any { it.assetId == content.id }
                }
            }
            ProviderSessionServices(
                profiles = profiles,
                home = home,
                details = details,
                search = search,
                playback = ClientBPlaybackSourceRepository(),
                contentPolicy = contentPolicy,
            )
        },
        local = PreferencesSdkStorage.create(storage.library, storage.search, storage.progress, Json { ignoreUnknownKeys = true; encodeDefaults = true }, authStore = storage.auth),
        closeResources = storage::close,
    )
}
