package com.pampoukidis.streamcoretv

import android.app.Application
import com.pampoukidis.streamcoretv.di.commonAndroidModules
import com.pampoukidis.streamcoretv.di.providerModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StreamCoreApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@StreamCoreApplication)
            modules(commonAndroidModules() + providerModules())
        }
    }
}
