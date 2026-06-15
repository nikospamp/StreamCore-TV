package com.pampoukidis.streamcoretv.data.tmdb.di

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.data.tmdb.profile.TmdbProfileRepository
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
