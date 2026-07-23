package com.pampoukidis.streamcoretv.client.clientb.ui.error

import com.pampoukidis.streamcoretv.core.model.error.ErrorPresentationMapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientBErrorPresentationModule {

    @Binds
    abstract fun bindErrorPresentationMapper(
        impl: ClientBErrorPresentationMapper,
    ): ErrorPresentationMapper
}
