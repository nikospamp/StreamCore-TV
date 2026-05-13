package com.pampoukidis.streamcoretv.data.clientb.di

import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import com.pampoukidis.streamcoretv.data.clientb.auth.ClientBAuthenticateRepository
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
