package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.auth.AppAuthViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::AppAuthViewModel)
}
