package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.BuildConfig
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbConnectionConfiguration
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbSdk
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbSdkConfiguration
import com.pampoukidis.streamcore.sdk.providers.tmdb.createAndroid
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import org.koin.android.ext.koin.androidContext
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.avatar.TmdbProfileAvatarArtworkResolver
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.error.TmdbErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.avatar.ProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.core.ui.error.DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER
import com.pampoukidis.streamcore.sdk.ui.error.ErrorPresentationMapper
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

private val tmdbRuntimeConfigModule = module {
    single {
        TmdbConnectionConfiguration(
            baseUrl = BuildConfig.TMDB_BASE_URL,
            readAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN,
        )
    }
}

private val tmdbProviderDataModule = module {
    includes(tmdbRuntimeConfigModule)
    single<StreamCoreClient> {
        TmdbSdk.createAndroid(
            androidContext(),
            TmdbSdkConfiguration(
                common = StreamCoreConfiguration(
                    backend = "tmdb-production", storageNamespace = "streamcore",
                    expectedAccountId = BuildConfig.TMDB_ACCOUNT_ID.takeIf { it.isNotBlank() },
                ),
                connection = get(), demoPlayback = true, legacyApplicationStorage = true,
            ),
        )
    } onClose { it?.close() }
}

private val tmdbProfileAvatarArtworkModule = module {
    single<ProfileAvatarArtworkResolver> { TmdbProfileAvatarArtworkResolver() }
}

private val tmdbErrorPresentationModule = module {
    single<ErrorPresentationMapper> {
        TmdbErrorPresentationMapper(get(named(DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER)))
    }
}

fun providerModules(): List<Module> {
    return listOf(
        tmdbProviderDataModule,
        tmdbProfileAvatarArtworkModule,
        tmdbErrorPresentationModule,
    )
}
