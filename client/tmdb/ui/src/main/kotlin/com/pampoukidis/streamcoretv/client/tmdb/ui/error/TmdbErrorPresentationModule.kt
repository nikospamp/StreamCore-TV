package com.pampoukidis.streamcoretv.client.tmdb.ui.error

import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbErrorPresentationModule {

    @Binds
    abstract fun bindErrorPresentationMapper(
        impl: TmdbErrorPresentationMapper,
    ): ErrorPresentationMapper
}