package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.repository.BusRepository
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

class WaitingBusViewModel(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _bus = MutableStateFlow<BusSummaryDto?>(null)
    val bus: StateFlow<BusSummaryDto?> = _bus

    private val _etaMinutes = MutableStateFlow<Int?>(null)
    val etaMinutes: StateFlow<Int?> = _etaMinutes

    private val _distanceMeters = MutableStateFlow<Int?>(null)
    val distanceMeters: StateFlow<Int?> = _distanceMeters

    private val _isArriving = MutableStateFlow(false)
    val isArriving: StateFlow<Boolean> = _isArriving

    private val _routePath = MutableStateFlow<List<LatLng>>(emptyList())
    val routePath: StateFlow<List<LatLng>> = _routePath

    private var pollingJob: Job? = null
    private var routeJob: Job? = null

    fun startTracking(plate: String, stop: StopDto) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                updateBusInfo(plate, stop)
                delay(10000)
            }
        }
    }

    private fun fetchRealRoute(plate: String, busLat: Double, busLng: Double, stop: StopDto) {
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            when (val result = busRepository.getBusRoute(plate)) {
                is ApiResult.Success -> {
                    val points = result.data.routes.firstOrNull()?.overviewPolyline?.points
                    if (points != null) {
                        val full = com.vibra.bus.util.decodePolyline(points)
                        _routePath.value = sliceToSegment(full, busLat, busLng, stop.latitude, stop.longitude)
                    } else {
                        _routePath.value = listOf(LatLng(busLat, busLng), LatLng(stop.latitude, stop.longitude))
                    }
                }
                else -> {
                    _routePath.value = listOf(LatLng(busLat, busLng), LatLng(stop.latitude, stop.longitude))
                }
            }
        }
    }

    /**
     * Extracts the sub-path of [path] between the point closest to the bus and
     * the point closest to the stop, then clamps both endpoints to exact coordinates.
     * This prevents drawing the full circular route and eliminates interleaving
     * from stale concurrent fetches (each fetch produces its own clean slice).
     */
    private fun sliceToSegment(
        path: List<LatLng>,
        busLat: Double, busLng: Double,
        stopLat: Double, stopLng: Double,
    ): List<LatLng> {
        if (path.size < 2) return listOf(LatLng(busLat, busLng), LatLng(stopLat, stopLng))

        val busIdx  = path.indices.minByOrNull { calculateDistance(path[it].latitude, path[it].longitude, busLat,  busLng)  } ?: 0
        val stopIdx = path.indices.minByOrNull { calculateDistance(path[it].latitude, path[it].longitude, stopLat, stopLng) } ?: path.lastIndex

        val (from, to) = if (busIdx <= stopIdx) busIdx to stopIdx else stopIdx to busIdx

        val slice = path.subList(from, to + 1).toMutableList()
        if (slice.isEmpty()) return listOf(LatLng(busLat, busLng), LatLng(stopLat, stopLng))

        // Clamp first point to exact bus position, last point to exact stop position
        slice[0]             = LatLng(busLat, busLng)
        slice[slice.lastIndex] = LatLng(stopLat, stopLng)
        return slice
    }

    private suspend fun updateBusInfo(plate: String, stop: StopDto) {
        when (val result = busRepository.getBuses(stop.latitude, stop.longitude)) {
            is ApiResult.Success -> {
                val foundBus = result.data.data.find { it.plate == plate }
                if (foundBus != null) {
                    val oldBus = _bus.value
                    val distance = if (oldBus != null)
                        calculateDistance(oldBus.latitude, oldBus.longitude, foundBus.latitude, foundBus.longitude)
                    else 0.0

                    // Prefer backend heading; fall back to client-side bearing when bus has moved >5m
                    val resolvedHeading = when {
                        foundBus.heading != 0 -> foundBus.heading
                        oldBus != null && distance > 5.0 ->
                            calculateBearing(oldBus.latitude, oldBus.longitude, foundBus.latitude, foundBus.longitude)
                        else -> _bus.value?.heading ?: 0
                    }
                    _bus.value = foundBus.copy(heading = resolvedHeading)

                    calculateMetrics(foundBus, stop)

                    // Solo recalculamos la ruta si el bus se ha movido significativamente o es la primera vez
                    if (oldBus == null || distance > 50) {
                        fetchRealRoute(plate, foundBus.latitude, foundBus.longitude, stop)
                    }
                }
            }
            else -> {}
        }
    }

    private fun calculateMetrics(bus: BusSummaryDto, stop: StopDto) {
        val distance = calculateDistance(bus.latitude, bus.longitude, stop.latitude, stop.longitude)
        _distanceMeters.value = distance.toInt()
        
        val estimatedMinutes = (distance / (8.3 * 60)).roundToInt()
        _etaMinutes.value = max(1, estimatedMinutes)
        
        if (distance < 200 && !_isArriving.value) {
            _isArriving.value = true
        }
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toInt()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val p1 = lat1 * PI / 180
        val p2 = lat2 * PI / 180
        val dp = (lat2 - lat1) * PI / 180
        val dl = (lon2 - lon1) * PI / 180

        val a = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    fun stopTracking() {
        pollingJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
