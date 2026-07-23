package com.pampoukidis.streamcoretv.client.tmdb.ui.avatar

import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbProfileAvatarArtworkModule {

    @Binds
    @Singleton
    abstract fun bindProfileAvatarArtworkResolver(
        impl: TmdbProfileAvatarArtworkResolver,
    ): ProfileAvatarArtworkResolver
}
