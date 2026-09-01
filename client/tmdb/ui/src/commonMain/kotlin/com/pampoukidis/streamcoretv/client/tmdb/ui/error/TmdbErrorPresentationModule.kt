package com.pampoukidis.streamcoretv.client.tmdb.ui.error

import com.pampoukidis.streamcoretv.core.ui.error.DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER
import com.pampoukidis.streamcoretv.core.ui.error.ErrorPresentationMapper
import org.koin.core.qualifier.named
import org.koin.dsl.module

val tmdbErrorPresentationModule = module {
    single<ErrorPresentationMapper> {
        TmdbErrorPresentationMapper(
            defaultMapper = get(named(DEFAULT_ERROR_PRESENTATION_MAPPER_QUALIFIER)),
        )
    }
}
