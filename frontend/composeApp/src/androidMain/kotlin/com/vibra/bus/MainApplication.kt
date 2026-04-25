package com.vibra.bus

import android.app.Application
import com.vibra.bus.di.commonModule
import com.vibra.bus.di.platformModule
import com.vibra.bus.util.initPlatformContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initPlatformContext(this)
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(commonModule, platformModule())
        }
    }
}
