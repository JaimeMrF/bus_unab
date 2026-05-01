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

    private var pollingJob: Job? = null

    fun startTracking(plate: String, stop: StopDto) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                // In a real app, we might get user location here too
                // For now, let's just get the bus position
                updateBusInfo(plate, stop)
                delay(10000) // Poll every 10 seconds to save battery and costs
            }
        }
    }

    private suspend fun updateBusInfo(plate: String, stop: StopDto) {
        // We use a dummy center since we just want the bus by plate
        when (val result = busRepository.getBuses(stop.latitude, stop.longitude)) {
            is ApiResult.Success -> {
                val foundBus = result.data.data.find { it.plate == plate }
                if (foundBus != null) {
                    _bus.value = foundBus
                    calculateMetrics(foundBus, stop)
                }
            }
            else -> {}
        }
    }

    private fun calculateMetrics(bus: BusSummaryDto, stop: StopDto) {
        val distance = calculateDistance(bus.latitude, bus.longitude, stop.latitude, stop.longitude)
        _distanceMeters.value = distance.toInt()
        
        // Simple ETA calculation: assuming 30km/h average in city (8.3 m/s)
        val estimatedMinutes = (distance / (8.3 * 60)).roundToInt()
        _etaMinutes.value = max(1, estimatedMinutes)
        
        // If less than 200 meters, it's arriving
        if (distance < 200 && !_isArriving.value) {
            _isArriving.value = true
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius in meters
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
