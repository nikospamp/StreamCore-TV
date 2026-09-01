package com.pampoukidis.streamcoretv.feature.details.domain

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val detailsDomainModule = module {
    factoryOf(::LoadDetailsUseCase)
}
