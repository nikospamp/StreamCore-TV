package com.pampoukidis.streamcoretv.client.clientb.data.di

import com.pampoukidis.streamcoretv.client.clientb.data.catalog.ClientBCatalogRepository
import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientBCatalogModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        impl: ClientBCatalogRepository,
    ): HomeRepository
}
