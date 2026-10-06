package com.pampoukidis.streamcore.sdk.runtime.session

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount

/** Identity of one authenticated account installation; compare instances to reject obsolete work. */
internal class AccountSession(val account: StreamCoreAuthAccount, val providers: ProviderSessionServices)
