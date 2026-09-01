package com.pampoukidis.streamcoretv.feature.search.common.search

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchUiModule = module {
    viewModelOf(::SearchViewModel)
}
