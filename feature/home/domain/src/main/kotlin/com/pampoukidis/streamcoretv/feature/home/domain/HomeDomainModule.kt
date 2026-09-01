package com.pampoukidis.streamcoretv.feature.home.domain

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val homeDomainModule = module {
    factoryOf(::LoadHomeRowsUseCase)
}
