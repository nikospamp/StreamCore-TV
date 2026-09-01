package com.pampoukidis.streamcoretv.playback.media3

import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val media3PlaybackModule = module {
    single<PlaybackSessionFactory> {
        Media3PlaybackSessionFactory(applicationContext = androidContext())
    }
}
