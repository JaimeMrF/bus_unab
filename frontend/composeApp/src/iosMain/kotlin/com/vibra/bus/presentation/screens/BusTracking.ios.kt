package com.vibra.bus.presentation.screens

actual fun startBusTracking(plate: String, stopLat: Double, stopLng: Double, stopName: String) {
    // iOS background tracking not implemented
}

actual fun stopBusTracking() {
    // no-op
}

actual fun isIgnoringBatteryOptimizations(): Boolean = true

actual fun openBatteryOptimizationSettings() {
    // no-op on iOS
}
