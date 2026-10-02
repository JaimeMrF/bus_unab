package com.vibra.bus.presentation.motion

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
actual fun platformAppActive(): Boolean {
    val owner = LocalContext.current as? LifecycleOwner ?: return true
    var active by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> active = true
                Lifecycle.Event.ON_PAUSE -> active = false
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return active
}

@Composable
actual fun platformReduceMotion(): Boolean {
    val context = LocalContext.current
    // Se relee cada vez que la app vuelve a primer plano (el usuario pudo cambiar el ajuste).
    val active = platformAppActive()
    return remember(active) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
actual fun platformLowTier(): Boolean {
    val context = LocalContext.current
    return remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        Build.VERSION.SDK_INT < 26 || am?.isLowRamDevice == true
    }
}
