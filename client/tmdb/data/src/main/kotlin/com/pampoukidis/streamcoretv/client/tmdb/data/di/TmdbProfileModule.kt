package com.pampoukidis.streamcoretv.client.tmdb.data.di

import com.pampoukidis.streamcoretv.client.tmdb.data.profile.TmdbProfileRepository
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: TmdbProfileRepository,
    ): ProfileRepository
}
