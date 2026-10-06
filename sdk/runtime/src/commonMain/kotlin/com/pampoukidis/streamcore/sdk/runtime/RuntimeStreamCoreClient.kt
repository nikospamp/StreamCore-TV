package com.pampoukidis.streamcore.sdk.runtime

import com.pampoukidis.streamcore.sdk.api.AuthService
import com.pampoukidis.streamcore.sdk.api.DetailsService
import com.pampoukidis.streamcore.sdk.api.HomeService
import com.pampoukidis.streamcore.sdk.api.LibraryService
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import com.pampoukidis.streamcore.sdk.runtime.auth.RuntimeAuthService
import com.pampoukidis.streamcore.sdk.runtime.profile.RuntimeProfileService
import com.pampoukidis.streamcore.sdk.runtime.home.RuntimeHomeService
import com.pampoukidis.streamcore.sdk.runtime.details.RuntimeDetailsService
import com.pampoukidis.streamcore.sdk.runtime.search.RuntimeSearchService
import com.pampoukidis.streamcore.sdk.runtime.library.RuntimeLibraryService
import com.pampoukidis.streamcore.sdk.runtime.playback.RuntimePlaybackService
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.auth.AuthProvider
import com.pampoukidis.streamcore.sdk.runtime.session.ProviderSessionFactory
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkLocalRepositories
import kotlinx.coroutines.flow.StateFlow

/**
 * Assembles one backend-agnostic SDK client. Domain services own their operations;
 * RuntimeSession owns the single lifecycle/account/profile state shared by those services.
 * Provider factories supply account-bound backend adapters and local persistence.
 */
class RuntimeStreamCoreClient(
    override val configuration: StreamCoreConfiguration,
    override val capabilities: StreamCoreCapabilities,
    authentication: AuthProvider,
    sessions: ProviderSessionFactory,
    local: SdkLocalRepositories,
    closeResources: () -> Unit = {},
) : StreamCoreClient {
    private val runtimeSession = RuntimeSession(configuration, authentication, closeResources)
    private val playbackService = RuntimePlaybackService(runtimeSession, capabilities, local.progress)

    override val context: StateFlow<StreamCoreContext> = runtimeSession.context
    override val auth: AuthService = RuntimeAuthService(runtimeSession, capabilities, authentication, sessions, local)
    override val profiles: ProfileService = RuntimeProfileService(runtimeSession, capabilities, local)
    override val home: HomeService = RuntimeHomeService(runtimeSession)
    override val details: DetailsService = RuntimeDetailsService(runtimeSession)
    override val search: SearchService = RuntimeSearchService(runtimeSession, capabilities, local)
    override val library: LibraryService = RuntimeLibraryService(runtimeSession, capabilities, local.library, playbackService)
    override val playback: PlaybackService = playbackService

    override fun close() {
        runtimeSession.close()
    }
}
