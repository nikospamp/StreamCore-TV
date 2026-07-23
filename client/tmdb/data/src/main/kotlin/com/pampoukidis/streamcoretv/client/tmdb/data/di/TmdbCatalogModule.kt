package com.pampoukidis.streamcoretv.client.tmdb.data.di

import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbCatalogRepository
import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbCatalogModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        impl: TmdbCatalogRepository,
    ): HomeRepository
}
