package com.pampoukidis.streamcoretv.client.tmdb.player

import android.content.Context
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import com.pampoukidis.streamcoretv.playback.media3.Media3PlaybackSessionFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbPlayerModule {
    @Binds
    @Singleton
    internal abstract fun bindPlaybackSourceRepository(
        implementation: TmdbPlaybackSourceRepository,
    ): PlaybackSourceRepository

    companion object {
        @Provides
        @Singleton
        fun providePlaybackSessionFactory(
            @ApplicationContext context: Context,
        ): PlaybackSessionFactory {
            return Media3PlaybackSessionFactory(context)
        }
    }
}