package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.client.clientb.data.di.clientBDataModule
import com.pampoukidis.streamcoretv.client.clientb.player.clientBPlayerModule
import com.pampoukidis.streamcoretv.client.clientb.ui.avatar.clientBProfileAvatarArtworkModule
import com.pampoukidis.streamcoretv.client.clientb.ui.error.clientBErrorPresentationModule
import org.koin.core.module.Module

fun providerModules(): List<Module> {
    return listOf(
        clientBDataModule,
        clientBPlayerModule,
        clientBProfileAvatarArtworkModule,
        clientBErrorPresentationModule,
    )
}
