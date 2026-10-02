package com.vibra.bus.presentation.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ProtectedQrDisplay() {
    DisposableEffect(Unit) {
        val previous = UIScreen.mainScreen.brightness
        UIScreen.mainScreen.brightness = 1.0
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose {
            UIScreen.mainScreen.brightness = previous
            UIApplication.sharedApplication.idleTimerDisabled = false
        }
    }
}
