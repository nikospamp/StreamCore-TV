package com.pampoukidis.streamcoretv.client.tmdb.data.di

import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbDetailsRepository
import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbDetailsModule {

    @Binds
    @Singleton
    abstract fun bindDetailsRepository(
        impl: TmdbDetailsRepository,
    ): DetailsRepository
}