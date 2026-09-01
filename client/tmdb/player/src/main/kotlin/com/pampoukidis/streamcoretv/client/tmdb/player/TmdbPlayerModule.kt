package com.pampoukidis.streamcoretv.client.tmdb.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import com.pampoukidis.streamcoretv.playback.media3.Media3PlaybackSessionFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val tmdbPlayerModule = module {
    single<PlaybackSourceRepository> { TmdbPlaybackSourceRepository() }
    single<PlaybackSessionFactory> {
        Media3PlaybackSessionFactory(applicationContext = androidContext())
    }
}
