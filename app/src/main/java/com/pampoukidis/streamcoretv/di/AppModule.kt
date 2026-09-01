package com.pampoukidis.streamcoretv.di

import com.pampoukidis.streamcoretv.auth.AppAuthViewModel
import com.pampoukidis.streamcoretv.core.tracing.AndroidPerformanceTracer
import com.pampoukidis.streamcoretv.core.tracing.BuildConfig as TracingBuildConfig
import com.pampoukidis.streamcoretv.core.tracing.NoOpPerformanceTracer
import com.pampoukidis.streamcoretv.core.tracing.PerformanceTracer
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single<PerformanceTracer> {
        if (TracingBuildConfig.ENABLED) AndroidPerformanceTracer else NoOpPerformanceTracer
    }
    viewModelOf(::AppAuthViewModel)
}
