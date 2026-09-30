package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.core.ui.error.coreUiModule
import com.pampoukidis.streamcoretv.feature.details.common.details.detailsUiModule
import com.pampoukidis.streamcoretv.feature.home.common.home.homeUiModule
import com.pampoukidis.streamcoretv.feature.library.common.library.libraryUiModule
import com.pampoukidis.streamcoretv.feature.login.common.login.loginUiModule
import com.pampoukidis.streamcoretv.feature.player.common.player.playerUiModule
import com.pampoukidis.streamcoretv.feature.profiles.common.profilesUiModule
import com.pampoukidis.streamcoretv.feature.search.common.search.searchUiModule
import com.pampoukidis.streamcoretv.playback.media3.media3PlaybackModule
import org.koin.core.module.Module

fun commonAndroidModules(): List<Module> {
    return listOf(
        appModule,
        sdkServicesModule,
        coreUiModule,
        loginUiModule,
        profilesUiModule,
        homeUiModule,
        searchUiModule,
        detailsUiModule,
        libraryUiModule,
        playerUiModule,
        media3PlaybackModule,
    )
}
