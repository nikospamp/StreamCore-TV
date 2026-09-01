package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.core.ui.error.coreUiModule
import com.pampoukidis.streamcoretv.feature.details.common.details.detailsUiModule
import com.pampoukidis.streamcoretv.feature.details.domain.detailsDomainModule
import com.pampoukidis.streamcoretv.feature.home.common.home.homeUiModule
import com.pampoukidis.streamcoretv.feature.home.domain.homeDomainModule
import com.pampoukidis.streamcoretv.feature.library.common.library.libraryUiModule
import com.pampoukidis.streamcoretv.feature.library.data.libraryDataModule
import com.pampoukidis.streamcoretv.feature.library.domain.libraryDomainModule
import com.pampoukidis.streamcoretv.feature.login.common.login.loginUiModule
import com.pampoukidis.streamcoretv.feature.login.domain.loginDomainModule
import com.pampoukidis.streamcoretv.feature.player.common.player.playerUiModule
import com.pampoukidis.streamcoretv.feature.player.data.playbackDataModule
import com.pampoukidis.streamcoretv.feature.profiles.common.profilesUiModule
import com.pampoukidis.streamcoretv.feature.profiles.domain.profilesDomainModule
import com.pampoukidis.streamcoretv.feature.search.common.search.searchUiModule
import com.pampoukidis.streamcoretv.feature.search.data.searchDataModule
import com.pampoukidis.streamcoretv.feature.search.domain.searchDomainModule
import org.koin.core.module.Module

fun commonAndroidModules(): List<Module> {
    return listOf(
        appModule,
        coreUiModule,
        loginDomainModule,
        loginUiModule,
        profilesDomainModule,
        profilesUiModule,
        homeDomainModule,
        homeUiModule,
        searchDataModule,
        searchDomainModule,
        searchUiModule,
        detailsDomainModule,
        detailsUiModule,
        libraryDataModule,
        libraryDomainModule,
        libraryUiModule,
        playbackDataModule,
        playerUiModule,
    )
}
