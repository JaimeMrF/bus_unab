package com.vibra.bus.util

data class LatLng(val latitude: Double, val longitude: Double)

expect class LocationManager {
    fun requestLocation(callback: (LatLng?) -> Unit)
    fun startLocationUpdates(callback: (LatLng) -> Unit)
    fun stopLocationUpdates()
}
