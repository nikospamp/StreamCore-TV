package com.pampoukidis.streamcoretv.feature.login.domain

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val loginDomainModule = module {
    factoryOf(::ValidateLoginCredentialsUseCase)
    factoryOf(::LoginWithCredentialsUseCase)
}
