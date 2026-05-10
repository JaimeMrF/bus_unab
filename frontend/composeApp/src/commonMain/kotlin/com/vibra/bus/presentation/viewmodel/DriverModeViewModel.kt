package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.BusCatalogItem
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.data.repository.BusRepository
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings
import com.vibra.bus.util.LatLng
import com.vibra.bus.util.LocationManager
import com.vibra.bus.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class LocationSource { BUS_GPS, PHONE_GPS }

class DriverModeViewModel(
    private val busRepository: BusRepository,
    private val settings: AppSettings,
    private val locationManager: LocationManager,
) : ViewModel() {

    private val _stopsState = MutableStateFlow<UiState<List<StopWithPivotDto>>>(UiState.Idle)
    val stopsState: StateFlow<UiState<List<StopWithPivotDto>>> = _stopsState

    private val _catalogState = MutableStateFlow<UiState<List<BusCatalogItem>>>(UiState.Idle)
    val catalogState: StateFlow<UiState<List<BusCatalogItem>>> = _catalogState

    private val _activePlate = MutableStateFlow(settings.driverActivePlate)
    val activePlate: StateFlow<String> = _activePlate

    private val _passengerCount = MutableStateFlow(0)
    val passengerCount: StateFlow<Int> = _passengerCount

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private val _isFull = MutableStateFlow(false)
    val isFull: StateFlow<Boolean> = _isFull

    private val _locationSource = MutableStateFlow(LocationSource.BUS_GPS)
    val locationSource: StateFlow<LocationSource> = _locationSource

    private var lastKnownLocation: LatLng? = null

    init {
        val stored = settings.driverActivePlate
        if (stored.isNotEmpty()) {
            loadStops(stored)
        } else {
            loadCatalog()
        }
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _catalogState.value = UiState.Loading
            when (val result = busRepository.getBusCatalog()) {
                is ApiResult.Success -> _catalogState.value = UiState.Success(result.data.data)
                is ApiResult.HttpError -> _catalogState.value = UiState.Error(result.message)
                is ApiResult.NetworkError -> _catalogState.value = UiState.Error("Sin conexión")
            }
        }
    }

    fun selectBus(item: BusCatalogItem) {
        settings.driverActivePlate = item.plate
        _activePlate.value = item.plate
        _catalogState.value = UiState.Idle
        loadStops(item.plate)
    }

    fun changeBus() {
        setLocationSource(LocationSource.BUS_GPS)
        settings.driverActivePlate = ""
        _activePlate.value = ""
        _stopsState.value = UiState.Idle
        loadCatalog()
    }

    fun loadStops(plate: String) {
        viewModelScope.launch {
            _stopsState.value = UiState.Loading
            when (val occupancyResult = busRepository.getBusOccupancy(plate)) {
                is ApiResult.Success -> {
                    val dto = occupancyResult.data.data
                    _isFull.value = (dto?.percentage ?: 0f) >= 100f
                    _passengerCount.value = dto?.currentOccupancy ?: 0
                }
                else -> {}
            }
            when (val result = busRepository.getBusStops(plate)) {
                is ApiResult.Success -> _stopsState.value = UiState.Success(result.data.data.sortedBy { it.order })
                is ApiResult.HttpError -> _stopsState.value = UiState.Error(result.message)
                is ApiResult.NetworkError -> {
                    _snackbarMessage.value = "Sin conexión a internet"
                    _stopsState.value = UiState.Error("Sin conexión")
                }
            }
        }
    }

    // ── Fuente de ubicación ───────────────────────────────────────────────────

    fun setLocationSource(source: LocationSource) {
        if (_locationSource.value == source) return
        _locationSource.value = source
        if (source == LocationSource.PHONE_GPS) {
            startPhoneGps()
        } else {
            stopPhoneGps()
            _snackbarMessage.value = "Usando GPS del bus"
        }
    }

    private fun startPhoneGps() {
        lastKnownLocation = null
        locationManager.startLocationUpdates { location ->
            val plate = _activePlate.value
            if (plate.isEmpty()) return@startLocationUpdates

            val heading = lastKnownLocation?.let {
                calculateBearing(it.latitude, it.longitude, location.latitude, location.longitude)
            } ?: 0
            lastKnownLocation = location

            viewModelScope.launch {
                busRepository.updateDriverLocation(plate, location.latitude, location.longitude, heading)
            }
        }
        _snackbarMessage.value = "Usando ubicación del teléfono"
    }

    private fun stopPhoneGps() {
        locationManager.stopLocationUpdates()
        lastKnownLocation = null
        val plate = _activePlate.value
        if (plate.isNotEmpty()) {
            viewModelScope.launch { busRepository.clearDriverLocation(plate) }
        }
    }

    // ── Acciones de conductor ─────────────────────────────────────────────────

    fun confirmArrival(plate: String, stopId: Int) {
        viewModelScope.launch {
            when (val result = busRepository.confirmArrival(plate, stopId)) {
                is ApiResult.Success -> {
                    val count = result.data.data?.notifiedUsers ?: 0
                    _snackbarMessage.value = "$count usuario(s) notificados"
                }
                is ApiResult.HttpError -> _snackbarMessage.value = result.message
                is ApiResult.NetworkError -> _snackbarMessage.value = "Sin conexión a internet"
            }
        }
    }

    fun notifyApproaching(plate: String, stopId: Int) {
        viewModelScope.launch {
            when (val result = busRepository.notifyApproaching(plate, stopId)) {
                is ApiResult.Success -> _snackbarMessage.value = "Usuarios avisados: bus aproximándose"
                is ApiResult.HttpError -> _snackbarMessage.value = result.message
                is ApiResult.NetworkError -> _snackbarMessage.value = "Sin conexión a internet"
            }
        }
    }

    fun toggleOccupancy(plate: String) {
        viewModelScope.launch {
            val newState = !_isFull.value
            when (val result = busRepository.updateBusOccupancy(plate, newState)) {
                is ApiResult.Success -> {
                    _isFull.value = newState
                    _passengerCount.value = result.data.data?.currentOccupancy ?: _passengerCount.value
                    _snackbarMessage.value = if (newState) "Bus reportado como LLENO" else "Bus reportado con ESPACIO"
                }
                is ApiResult.HttpError -> _snackbarMessage.value = result.message
                is ApiResult.NetworkError -> _snackbarMessage.value = "Sin conexión a internet"
            }
        }
    }

    fun consumeSnackbar() { _snackbarMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        stopPhoneGps()
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1R = Math.toRadians(lat1)
        val lat2R = Math.toRadians(lat2)
        val y = sin(dLon) * cos(lat2R)
        val x = cos(lat1R) * sin(lat2R) - sin(lat1R) * cos(lat2R) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toInt()
    }
}
