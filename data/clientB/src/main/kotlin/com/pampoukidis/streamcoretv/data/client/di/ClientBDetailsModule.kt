package com.pampoukidis.streamcoretv.data.client.di

import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.data.client.catalog.ClientBDetailsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientBDetailsModule {

    @Binds
    @Singleton
    abstract fun bindDetailsRepository(
        impl: ClientBDetailsRepository,
    ): DetailsRepository
}