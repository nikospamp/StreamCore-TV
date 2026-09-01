package com.pampoukidis.streamcoretv.client.clientb.ui.avatar

import com.pampoukidis.streamcoretv.core.ui.avatar.ProfileAvatarArtworkResolver
import org.koin.dsl.module

val clientBProfileAvatarArtworkModule = module {
    single<ProfileAvatarArtworkResolver> { ClientBProfileAvatarArtworkResolver() }
}
