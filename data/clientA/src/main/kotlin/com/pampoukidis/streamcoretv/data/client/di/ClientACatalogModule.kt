package com.pampoukidis.streamcoretv.data.client.di

import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.data.client.catalog.ClientACatalogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientACatalogModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        impl: ClientACatalogRepository,
    ): HomeRepository
}
