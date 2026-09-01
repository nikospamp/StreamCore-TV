package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.BuildConfig
import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import com.pampoukidis.streamcoretv.client.tmdb.data.di.tmdbAndroidDataModule
import com.pampoukidis.streamcoretv.client.tmdb.data.di.tmdbDataModule
import com.pampoukidis.streamcoretv.client.tmdb.player.tmdbPlayerModule
import com.pampoukidis.streamcoretv.client.tmdb.ui.avatar.tmdbProfileAvatarArtworkModule
import com.pampoukidis.streamcoretv.client.tmdb.ui.error.tmdbErrorPresentationModule
import org.koin.core.module.Module
import org.koin.dsl.module

private val tmdbRuntimeConfigModule = module {
    single {
        TmdbRuntimeConfig(
            baseUrl = BuildConfig.TMDB_BASE_URL,
            readAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN,
            accountId = BuildConfig.TMDB_ACCOUNT_ID,
        )
    }
}

private val tmdbProviderDataModule = module {
    includes(
        tmdbRuntimeConfigModule,
        tmdbDataModule,
        tmdbAndroidDataModule,
    )
}

fun providerModules(): List<Module> {
    return listOf(
        tmdbProviderDataModule,
        tmdbPlayerModule,
        tmdbProfileAvatarArtworkModule,
        tmdbErrorPresentationModule,
    )
}
