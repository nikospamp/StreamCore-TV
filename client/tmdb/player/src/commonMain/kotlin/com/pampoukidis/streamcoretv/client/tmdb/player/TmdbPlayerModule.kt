package com.pampoukidis.streamcoretv.client.tmdb.player

import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import org.koin.dsl.module

val tmdbPlayerModule = module {
    single<PlaybackSourceRepository> { TmdbPlaybackSourceRepository() }
}
