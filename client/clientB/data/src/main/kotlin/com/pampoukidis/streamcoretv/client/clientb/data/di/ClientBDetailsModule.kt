package com.pampoukidis.streamcoretv.client.clientb.data.di

import com.pampoukidis.streamcoretv.client.clientb.data.catalog.ClientBDetailsRepository
import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
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