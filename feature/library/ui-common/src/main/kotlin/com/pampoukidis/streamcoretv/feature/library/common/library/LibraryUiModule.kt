package com.pampoukidis.streamcoretv.feature.library.common.library

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val libraryUiModule = module {
    viewModelOf(::LibraryViewModel)
}
