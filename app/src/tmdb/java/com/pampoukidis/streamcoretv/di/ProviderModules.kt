package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.client.tmdb.data.di.tmdbDataModule
import com.pampoukidis.streamcoretv.client.tmdb.player.tmdbPlayerModule
import com.pampoukidis.streamcoretv.client.tmdb.ui.avatar.tmdbProfileAvatarArtworkModule
import com.pampoukidis.streamcoretv.client.tmdb.ui.error.tmdbErrorPresentationModule
import org.koin.core.module.Module

fun providerModules(): List<Module> {
    return listOf(
        tmdbDataModule,
        tmdbPlayerModule,
        tmdbProfileAvatarArtworkModule,
        tmdbErrorPresentationModule,
    )
}
