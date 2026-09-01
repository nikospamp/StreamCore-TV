package com.pampoukidis.streamcoretv.feature.home.common.home

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeUiModule = module {
    viewModelOf(::HomeViewModel)
}
