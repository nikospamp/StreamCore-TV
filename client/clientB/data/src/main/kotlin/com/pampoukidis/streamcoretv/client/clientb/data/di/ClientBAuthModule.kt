package com.pampoukidis.streamcoretv.client.clientb.data.di

import com.pampoukidis.streamcoretv.client.clientb.data.auth.ClientBAuthenticateRepository
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientBAuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthenticateRepository(
        impl: ClientBAuthenticateRepository,
    ): AuthenticateRepository
}