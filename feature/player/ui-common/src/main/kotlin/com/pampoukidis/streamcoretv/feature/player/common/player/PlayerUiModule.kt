package com.pampoukidis.streamcoretv.feature.player.common.player

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playerUiModule = module {
    viewModelOf(::PlayerViewModel)
}
