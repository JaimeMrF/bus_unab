package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.BusCatalogItem
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.data.repository.BusRepository
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings
import com.vibra.bus.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DriverModeViewModel(
    private val busRepository: BusRepository,
    private val settings: AppSettings,
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

    fun confirmArrival(plate: String, stopId: Int) {
        viewModelScope.launch {
            when (val result = busRepository.confirmArrival(plate, stopId)) {
                is ApiResult.Success -> {
                    val count = result.data.data.notifiedUsers
                    _snackbarMessage.value = "$count usuario(s) notificados"
                }
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
}
