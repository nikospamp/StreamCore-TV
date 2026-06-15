package com.pampoukidis.streamcoretv.data.tmdb.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the TMDB endpoint contract to the Ktor-backed implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class TmdbApiModule {

    @Binds
    abstract fun bindTmdbApi(
        impl: KtorTmdbApi,
    ): TmdbApi
}
