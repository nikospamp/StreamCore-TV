package com.pampoukidis.streamcoretv.client.clientb.ui.avatar

import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientBProfileAvatarArtworkModule {

    @Binds
    @Singleton
    abstract fun bindProfileAvatarArtworkResolver(
        impl: ClientBProfileAvatarArtworkResolver,
    ): ProfileAvatarArtworkResolver
}
