package com.vibra.bus.presentation.viewmodel

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.OccupancyDto
import com.vibra.bus.data.model.PoiDto
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.data.repository.BusRepository
import com.vibra.bus.data.repository.PoiRepository
import com.vibra.bus.data.repository.StopRepository
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings
import com.vibra.bus.util.LatLng
import com.vibra.bus.util.LocationManager
import com.vibra.bus.util.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val busRepository: BusRepository,
    private val stopRepository: StopRepository,
    private val poiRepository: PoiRepository,
    private val locationManager: LocationManager,
    private val settings: AppSettings,
) : ViewModel() {

    private val _busesState = MutableStateFlow<UiState<List<BusSummaryDto>>>(UiState.Loading)
    val busesState: StateFlow<UiState<List<BusSummaryDto>>> = _busesState

    private val _stopsState = MutableStateFlow<UiState<List<StopDto>>>(UiState.Loading)
    val stopsState: StateFlow<UiState<List<StopDto>>> = _stopsState

    private val _poisState = MutableStateFlow<UiState<List<PoiDto>>>(UiState.Idle)
    val poisState: StateFlow<UiState<List<PoiDto>>> = _poisState

    private val _occupancyMap = MutableStateFlow<Map<String, OccupancyDto>>(emptyMap())
    val occupancyMap: StateFlow<Map<String, OccupancyDto>> = _occupancyMap

    private val _busStops = MutableStateFlow<List<StopWithPivotDto>>(emptyList())
    val busStops: StateFlow<List<StopWithPivotDto>> = _busStops

    private val _busStopsLoading = MutableStateFlow(false)
    val busStopsLoading: StateFlow<Boolean> = _busStopsLoading

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired

    private var pollingJob: Job? = null
    // Ubicación por defecto hasta tener GPS del usuario
    private var currentLocation = LatLng(7.1166, -73.1051) 

    init {
        loadStops()
    }

    fun startPolling() {
        pollingJob?.cancel()
        locationManager.startLocationUpdates { updateLocation(it) }
        pollingJob = viewModelScope.launch {
            var cycle = 0
            while (true) {
                loadBusesOnce(withOccupancy = cycle % OCCUPANCY_EVERY == 0)
                cycle++
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        locationManager.stopLocationUpdates()
    }

    fun loadBusStops(plate: String) {
        viewModelScope.launch {
            _busStops.value = emptyList()
            _busStopsLoading.value = true
            when (val result = busRepository.getBusStops(plate)) {
                is ApiResult.Success -> _busStops.value = result.data.data.sortedBy { it.order }
                else -> {}
            }
            _busStopsLoading.value = false
        }
    }

    fun clearBusStops() {
        _busStops.value = emptyList()
        _busStopsLoading.value = false
    }

    fun updateLocation(latLng: LatLng) {
        currentLocation = latLng
        _userLocation.value = latLng
    }

    private fun loadBuses() {
        viewModelScope.launch { loadBusesOnce(withOccupancy = true) }
    }

    /** Una consulta de buses. Es suspend para que el bucle de polling no solape peticiones lentas. */
    private suspend fun loadBusesOnce(withOccupancy: Boolean) {
        val wasError = _busesState.value is UiState.Error
        when (val result = busRepository.getBuses(currentLocation.latitude, currentLocation.longitude)) {
            is ApiResult.Success -> {
                // StateFlow descarta valores iguales: si nada cambio, no hay recomposicion.
                _busesState.value = UiState.Success(result.data.data)
                if (withOccupancy) loadOccupancy(result.data.data.map { it.plate })
            }
            is ApiResult.HttpError -> {
                when (result.code) {
                    401 -> { settings.clearSession(); _sessionExpired.value = true }
                    503 -> if (!wasError) _snackbarMessage.value = "GPS no disponible temporalmente"
                    else -> _busesState.value = UiState.Error(result.message)
                }
            }
            is ApiResult.NetworkError -> {
                // El aviso solo sale al pasar de "bien" a "sin red", no en cada ciclo.
                if (!wasError) _snackbarMessage.value = "Sin conexión a internet"
                _busesState.value = UiState.Error(result.message)
            }
        }
    }

    /** Ocupacion de todos los buses en paralelo (antes era una peticion tras otra). */
    private suspend fun loadOccupancy(plates: List<String>) = coroutineScope {
        val results = plates.map { plate -> async { plate to busRepository.getBusOccupancy(plate) } }.awaitAll()
        val newMap = mutableMapOf<String, OccupancyDto>()
        for ((plate, result) in results) {
            if (result is ApiResult.Success) result.data.data?.let { newMap[plate] = it }
        }
        _occupancyMap.value = newMap
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
        const val OCCUPANCY_EVERY = 3
    }

    private fun loadStops() {
        viewModelScope.launch {
            when (val result = stopRepository.getStops()) {
                is ApiResult.Success -> _stopsState.value = UiState.Success(result.data.data)
                is ApiResult.HttpError -> _stopsState.value = UiState.Error(result.message)
                is ApiResult.NetworkError -> _snackbarMessage.value = "Sin conexión a internet"
            }
        }
    }

    fun loadPois() {
        viewModelScope.launch {
            when (val result = poiRepository.getPois(currentLocation.latitude, currentLocation.longitude)) {
                is ApiResult.Success -> _poisState.value = UiState.Success(result.data.data)
                is ApiResult.HttpError -> {}
                is ApiResult.NetworkError -> {}
            }
        }
    }

    fun refresh() {
        loadBuses()
        loadStops()
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }
}
