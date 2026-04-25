package com.vibra.bus.util

import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class LocationManager {

    private val clManager = CLLocationManager()

    actual fun requestLocation(callback: (LatLng?) -> Unit) {
        clManager.requestWhenInUseAuthorization()
        val location = clManager.location
        if (location != null) {
            callback(LatLng(location.coordinate.useContents { latitude }, location.coordinate.useContents { longitude }))
        } else {
            callback(null)
        }
    }

    actual fun startLocationUpdates(callback: (LatLng) -> Unit) {
        clManager.desiredAccuracy = kCLLocationAccuracyBest
        clManager.requestWhenInUseAuthorization()
        clManager.startUpdatingLocation()
    }

    actual fun stopLocationUpdates() {
        clManager.stopUpdatingLocation()
    }
}
