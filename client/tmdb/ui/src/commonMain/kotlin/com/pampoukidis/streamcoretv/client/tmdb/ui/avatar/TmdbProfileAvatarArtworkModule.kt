package com.pampoukidis.streamcoretv.client.tmdb.ui.avatar

import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import org.koin.dsl.module

val tmdbProfileAvatarArtworkModule = module {
    single<ProfileAvatarArtworkResolver> { TmdbProfileAvatarArtworkResolver() }
}
