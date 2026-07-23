package com.pampoukidis.streamcoretv.client.tmdb.data.di

import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TmdbAuthenticateRepository
import com.pampoukidis.streamcoretv.core.domain.AuthenticateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbAuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthenticateRepository(
        impl: TmdbAuthenticateRepository,
    ): AuthenticateRepository
}