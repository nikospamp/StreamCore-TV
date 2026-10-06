package com.pampoukidis.streamcore.sdk.model

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile

/**
 * Atomic account/authorization view for one client.
 * 
 * @property account Discovered identity, exposed after required account installation succeeds; profile entry is separate.
 * @property profile Currently authorized profile, null before entry and after access is revoked.
 * @property isAuthInitialized Whether account restoration/installation completed, including a resolved logged-out state.
 * This does not mean an account is signed in; inspect [account] and operation failures separately.
 * @property isClosed Whether the client released its resources. Its StateFlow remains host-collected.
 * @property profileActivationId In-memory authorization identity, also changing when active profile data changes.
 * Use it to replace profile-scoped presentation and recreate SDK observers/recorders; never persist it as a grant.
 */
data class StreamCoreContext(
    val account: StreamCoreAuthAccount? = null,
    val profile: StreamCoreProfile? = null,
    val isAuthInitialized: Boolean = false,
    val isClosed: Boolean = false,
    /** Changes on every authorization activation; never persist or treat it as a backend token. */
    val profileActivationId: String? = null,
)
