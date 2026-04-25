package com.vibra.bus.di

import com.vibra.bus.util.GoogleSignInManager
import com.vibra.bus.util.LocationManager
import com.vibra.bus.util.QRScannerManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { LocationManager(androidContext()) }
    single { GoogleSignInManager(androidContext()) }
    single { QRScannerManager(androidContext()) }
}
