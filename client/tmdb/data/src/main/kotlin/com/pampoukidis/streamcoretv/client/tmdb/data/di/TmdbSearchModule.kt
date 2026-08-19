package com.pampoukidis.streamcoretv.client.tmdb.data.di

import com.pampoukidis.streamcoretv.client.tmdb.data.search.TmdbSearchRepository
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbSearchModule {

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: TmdbSearchRepository,
    ): SearchRepository
}
