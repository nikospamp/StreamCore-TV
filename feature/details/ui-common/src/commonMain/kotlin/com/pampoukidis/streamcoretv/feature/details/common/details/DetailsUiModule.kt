package com.pampoukidis.streamcoretv.feature.details.common.details

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val detailsUiModule = module {
    viewModelOf(::DetailsViewModel)
}
