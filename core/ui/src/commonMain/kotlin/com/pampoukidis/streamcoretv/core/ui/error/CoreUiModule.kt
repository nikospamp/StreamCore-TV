package com.pampoukidis.streamcoretv.core.ui.error

import com.pampoukidis.streamcore.sdk.ui.error.DefaultErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.error.ErrorPresentationMapper
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER = "defaultErrorPresentationMapper"

val coreUiModule = module {
    single<ErrorPresentationMapper>(named(DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER)) {
        DefaultErrorPresentationMapper()
    }
}
