package com.vibra.bus.presentation.screens

import android.content.Context
import android.content.Intent
import android.os.Build
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
