package com.pampoukidis.streamcoretv.data.tmdb.di

import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.data.tmdb.catalog.TmdbCatalogRepository
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
