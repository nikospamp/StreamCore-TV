package com.pampoukidis.streamcoretv.client.clientb.data.di

import com.pampoukidis.streamcoretv.client.clientb.data.auth.ClientBAuthStore
import com.pampoukidis.streamcoretv.client.clientb.data.auth.ClientBAuthenticateRepository
import com.pampoukidis.streamcoretv.client.clientb.data.auth.ClientBPreferencesAuthStore
import com.pampoukidis.streamcoretv.client.clientb.data.catalog.ClientBCatalogRepository
import com.pampoukidis.streamcoretv.client.clientb.data.catalog.ClientBCatalogSource
import com.pampoukidis.streamcoretv.client.clientb.data.catalog.ClientBDetailsRepository
import com.pampoukidis.streamcoretv.client.clientb.data.catalog.ClientBSearchRepository
import com.pampoukidis.streamcoretv.client.clientb.data.profile.ClientBProfileRepository
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

const val CLIENT_B_AUTH_STORE_QUALIFIER = "clientBAuthStore"

val clientBDataModule = module {
    single<ClientBAuthStore> {
        ClientBPreferencesAuthStore(
            dataStore = get(named(CLIENT_B_AUTH_STORE_QUALIFIER)),
        )
    }
    single<AuthenticateRepository> {
        ClientBAuthenticateRepository(authStore = get())
    }
    single { ClientBCatalogSource() }
    single { ClientBCatalogRepository(catalogSource = get()) } bind HomeRepository::class
    single { ClientBDetailsRepository(catalogSource = get()) } bind DetailsRepository::class
    single { ClientBProfileRepository() } bind ProfileRepository::class
    single {
        ClientBSearchRepository(
            catalogSource = get(),
            profileRepository = get(),
        )
    } bind SearchRepository::class
}
