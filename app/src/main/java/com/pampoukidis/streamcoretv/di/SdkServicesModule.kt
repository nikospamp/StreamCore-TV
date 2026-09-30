package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcore.sdk.api.*
import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.api.LibraryService
import org.koin.dsl.module

internal val sdkServicesModule = module {
    single<LibraryService> { get<StreamCoreClient>().library }
    single<AuthService> { get<StreamCoreClient>().auth }
    single<ProfileService> { get<StreamCoreClient>().profiles }
    single<HomeService> { get<StreamCoreClient>().home }
    single<DetailsService> { get<StreamCoreClient>().details }
    single<SearchService> { get<StreamCoreClient>().search }
    single<PlaybackService> { get<StreamCoreClient>().playback }
}
