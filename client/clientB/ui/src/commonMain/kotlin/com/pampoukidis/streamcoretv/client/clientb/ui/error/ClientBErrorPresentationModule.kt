package com.pampoukidis.streamcoretv.client.clientb.ui.error

import com.pampoukidis.streamcoretv.core.ui.error.DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER
import com.pampoukidis.streamcoretv.core.ui.error.ErrorPresentationMapper
import org.koin.core.qualifier.named
import org.koin.dsl.module

val clientBErrorPresentationModule = module {
    single<ErrorPresentationMapper> {
        ClientBErrorPresentationMapper(
            defaultMapper = get(named(DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER)),
        )
    }
}
