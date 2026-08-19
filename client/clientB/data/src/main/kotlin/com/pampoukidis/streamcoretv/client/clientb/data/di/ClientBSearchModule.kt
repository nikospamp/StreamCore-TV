package com.pampoukidis.streamcoretv.client.clientb.data.di

import com.pampoukidis.streamcoretv.client.clientb.data.catalog.ClientBSearchRepository
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientBSearchModule {

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: ClientBSearchRepository,
    ): SearchRepository
}
