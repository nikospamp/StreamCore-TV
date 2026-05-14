package com.pampoukidis.streamcoretv.core.ui.error

import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
abstract class DefaultErrorPresentationModule {

    @Binds
    @Named("defaultErrorPresentationMapper")
    abstract fun bindDefaultMapper(
        impl: DefaultErrorPresentationPresentationMapper,
    ): ErrorPresentationMapper
}