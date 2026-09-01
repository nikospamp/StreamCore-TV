package com.pampoukidis.streamcoretv.feature.search.domain

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val searchDomainModule = module {
    factoryOf(::AddRecentSearchUseCase)
    factoryOf(::ClearRecentSearchesUseCase)
    factoryOf(::LoadSearchDiscoveryUseCase)
    factoryOf(::ObserveRecentSearchesUseCase)
    factoryOf(::RemoveRecentSearchUseCase)
    factoryOf(::SearchContentUseCase)
}
