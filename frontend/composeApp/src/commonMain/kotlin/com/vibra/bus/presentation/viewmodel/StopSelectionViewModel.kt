package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.BusDetailDto
import com.vibra.bus.data.model.CreateRequestData
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.data.repository.BusRepository
import com.vibra.bus.data.repository.RequestRepository
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StopSelectionViewModel(
    private val busRepository: BusRepository,
    private val requestRepository: RequestRepository,
) : ViewModel() {

    private val _stopsState = MutableStateFlow<UiState<List<StopWithPivotDto>>>(UiState.Loading)
    val stopsState: StateFlow<UiState<List<StopWithPivotDto>>> = _stopsState

    private val _busDetail = MutableStateFlow<BusDetailDto?>(null)
    val busDetail: StateFlow<BusDetailDto?> = _busDetail

    private val _selectedStop = MutableStateFlow<StopWithPivotDto?>(null)
    val selectedStop: StateFlow<StopWithPivotDto?> = _selectedStop

    private val _requestState = MutableStateFlow<UiState<CreateRequestData>>(UiState.Idle)
    val requestState: StateFlow<UiState<CreateRequestData>> = _requestState

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    fun loadStops(plate: String) {
        viewModelScope.launch {
            _stopsState.value = UiState.Loading
            when (val result = busRepository.getBusStops(plate)) {
                is ApiResult.Success -> {
                    val stops = result.data.data.sortedBy { it.order }
                    _stopsState.value = UiState.Success(stops)
                    if (stops.isNotEmpty()) _selectedStop.value = stops.first()
                }
                is ApiResult.HttpError -> _stopsState.value = UiState.Error(result.message)
                is ApiResult.NetworkError -> {
                    _snackbarMessage.value = result.message
                    _stopsState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun loadBusDetail(plate: String) {
        viewModelScope.launch {
            when (val result = busRepository.getBusDetail(plate)) {
                is ApiResult.Success -> _busDetail.value = result.data.data
                else -> {}
            }
        }
    }

    fun selectStop(stop: StopWithPivotDto) {
        _selectedStop.value = stop
    }

    fun confirmStop(busId: Int, stopId: Int) {
        viewModelScope.launch {
            _requestState.value = UiState.Loading
            when (val result = requestRepository.createRequest(busId, stopId)) {
                is ApiResult.Success -> {
                    val data = result.data.data
                    if (data != null) _requestState.value = UiState.Success(data)
                    else _requestState.value = UiState.Error("Respuesta inválida del servidor")
                }
                is ApiResult.HttpError -> {
                    if (result.code == 422) {
                        _requestState.value = UiState.Error("Error de validación")
                    } else {
                        _requestState.value = UiState.Error(result.message)
                    }
                }
                is ApiResult.NetworkError -> {
                    _snackbarMessage.value = result.message
                    _requestState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun consumeSnackbar() { _snackbarMessage.value = null }
    fun consumeRequestState() { _requestState.value = UiState.Idle }
}
