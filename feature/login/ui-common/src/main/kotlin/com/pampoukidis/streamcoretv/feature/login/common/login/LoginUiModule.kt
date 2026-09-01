package com.pampoukidis.streamcoretv.feature.login.common.login

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val loginUiModule = module {
    viewModelOf(::LoginViewModel)
}
