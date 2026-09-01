package com.pampoukidis.streamcoretv.client.clientb.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import com.pampoukidis.streamcoretv.playback.media3.Media3PlaybackSessionFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val clientBPlayerModule = module {
    single<PlaybackSourceRepository> { ClientBPlaybackSourceRepository() }
    single<PlaybackSessionFactory> {
        Media3PlaybackSessionFactory(applicationContext = androidContext())
    }
}
