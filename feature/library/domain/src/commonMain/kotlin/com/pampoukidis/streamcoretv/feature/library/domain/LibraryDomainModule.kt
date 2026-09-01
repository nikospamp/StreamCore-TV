package com.pampoukidis.streamcoretv.feature.library.domain

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val libraryDomainModule = module {
    single<LibraryClock> { SystemLibraryClock }
    factoryOf(::ObserveContentLibraryStateUseCase)
    factoryOf(::ObserveLibraryUseCase)
    factoryOf(::SetContentInMyListUseCase)
    factoryOf(::SetContentLikedUseCase)
}
