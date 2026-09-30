package com.pampoukidis.streamcore.sdk.runtime.integration.provider

import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount

fun interface ProviderSessionFactory {
    /** Create account-bound adapters; must not perform network requests. */
    fun create(account: StreamCoreAuthAccount): ProviderSessionServices
}
