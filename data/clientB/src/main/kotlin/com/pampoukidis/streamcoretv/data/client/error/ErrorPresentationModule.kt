package com.pampoukidis.streamcoretv.data.client.error

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorPresentationModule {

    @Binds
    abstract fun bindErrorPresentationMapper(
        impl: ErrorPresentationPresentationMapper,
    ): ErrorPresentationPresentationMapper
}