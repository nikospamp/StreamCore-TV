package com.pampoukidis.streamcoretv.data.client.di

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.data.client.profile.ClientAProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientAProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ClientAProfileRepository,
    ): ProfileRepository
}
