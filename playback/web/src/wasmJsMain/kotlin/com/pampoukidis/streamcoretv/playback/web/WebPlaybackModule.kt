package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import org.koin.dsl.module

val webPlaybackModule = module {
    single<PlaybackSessionFactory> {
        WebPlaybackSessionFactory()
    }
}
