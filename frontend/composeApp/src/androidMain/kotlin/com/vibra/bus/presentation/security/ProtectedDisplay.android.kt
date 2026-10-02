package com.vibra.bus.presentation.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun ProtectedQrDisplay() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        val previous = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        window?.let { w ->
            w.attributes = w.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        }
        onDispose {
            window?.clearFlags(
                WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            window?.let { w -> w.attributes = w.attributes.also { it.screenBrightness = previous } }
        }
    }
}
