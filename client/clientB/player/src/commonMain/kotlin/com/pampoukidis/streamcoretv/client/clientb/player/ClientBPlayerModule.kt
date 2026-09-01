package com.pampoukidis.streamcoretv.client.clientb.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import org.koin.dsl.module

val clientBPlayerModule = module {
    single<PlaybackSourceRepository> { ClientBPlaybackSourceRepository() }
}
