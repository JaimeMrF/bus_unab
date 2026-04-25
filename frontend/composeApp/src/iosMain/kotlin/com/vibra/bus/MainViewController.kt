package com.vibra.bus

import androidx.compose.ui.window.ComposeUIViewController
import com.vibra.bus.di.commonModule
import com.vibra.bus.di.platformModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController { App() }

fun initKoin() {
    startKoin {
        modules(commonModule, platformModule())
    }
}
