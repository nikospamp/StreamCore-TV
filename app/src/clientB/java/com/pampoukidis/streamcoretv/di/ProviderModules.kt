package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.BuildConfig
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBReferenceProfileScenario

import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdk
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdkConfiguration
import com.pampoukidis.streamcore.sdk.providers.clientb.createAndroid
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import org.koin.android.ext.koin.androidContext
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.avatar.ClientBProfileAvatarArtworkResolver
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.error.ClientBErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.avatar.ProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.core.ui.error.DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER
import com.pampoukidis.streamcore.sdk.ui.error.ErrorPresentationMapper
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

private val clientBProviderDataModule = module {
    single<StreamCoreClient> {
        ClientBSdk.createAndroid(
            androidContext(),
            ClientBSdkConfiguration(
                common = StreamCoreConfiguration(backend = "clientb-simulated", storageNamespace = "streamcore"),
                demoPlayback = true,
                referenceProfileScenario = ClientBReferenceProfileScenario.valueOf(BuildConfig.CLIENTB_REFERENCE_PROFILE_SCENARIO),
            ),
        )
    } onClose { it?.close() }
}

private val clientBProfileAvatarArtworkModule = module {
    single<ProfileAvatarArtworkResolver> { ClientBProfileAvatarArtworkResolver() }
}

private val clientBErrorPresentationModule = module {
    single<ErrorPresentationMapper> {
        ClientBErrorPresentationMapper(get(named(DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER)))
    }
}

fun providerModules(): List<Module> {
    return listOf(
        clientBProviderDataModule,
        clientBProfileAvatarArtworkModule,
        clientBErrorPresentationModule,
    )
}
