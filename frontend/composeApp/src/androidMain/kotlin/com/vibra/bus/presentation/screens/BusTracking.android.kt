package com.vibra.bus.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.vibra.bus.BusTrackingService
import org.koin.core.context.GlobalContext

private fun appContext(): Context =
    GlobalContext.get().get()

actual fun startBusTracking(plate: String, stopLat: Double, stopLng: Double, stopName: String) {
    val ctx = appContext()
    val intent = Intent(ctx, BusTrackingService::class.java).apply {
        putExtra(BusTrackingService.EXTRA_PLATE,     plate)
        putExtra(BusTrackingService.EXTRA_STOP_LAT,  stopLat)
        putExtra(BusTrackingService.EXTRA_STOP_LNG,  stopLng)
        putExtra(BusTrackingService.EXTRA_STOP_NAME, stopName)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ctx.startForegroundService(intent)
    } else {
        ctx.startService(intent)
    }
}

actual fun stopBusTracking() {
    val ctx = appContext()
    ctx.stopService(Intent(ctx, BusTrackingService::class.java))
}

actual fun isIgnoringBatteryOptimizations(): Boolean {
    val ctx = appContext()
    val pm = ctx.getSystemService(PowerManager::class.java) ?: return true
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

actual fun openBatteryOptimizationSettings() {
    val ctx = appContext()
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data  = Uri.parse("package:${ctx.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    } catch (_: Exception) {}
}
