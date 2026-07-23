package com.pampoukidis.streamcoretv.client.clientb.data.di

import com.pampoukidis.streamcoretv.client.clientb.data.profile.ClientBProfileRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientBProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ClientBProfileRepository,
    ): ProfileRepository
}
