package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.RequestInfo
import com.vibra.bus.data.repository.RequestRepository
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings
import com.vibra.bus.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyTripsViewModel(
    private val requestRepository: RequestRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _tripsState = MutableStateFlow<UiState<List<RequestInfo>>>(UiState.Loading)
    val tripsState: StateFlow<UiState<List<RequestInfo>>> = _tripsState

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    init {
        loadLocalTrip()
    }

    private fun loadLocalTrip() {
        val (reqId, busId, stopId) = requestRepository.getActiveRequest()
        if (reqId != -1) {
            _tripsState.value = UiState.Success(
                listOf(
                    RequestInfo(
                        id = reqId,
                        status = "pending",
                        bus = com.vibra.bus.data.model.RequestBusInfo(busId, "Bus activo", ""),
                        stop = com.vibra.bus.data.model.RequestStopInfo(stopId, "Parada seleccionada", ""),
                    )
                )
            )
        } else {
            _tripsState.value = UiState.Success(emptyList())
        }
    }

    fun cancelTrip(busId: Int) {
        viewModelScope.launch {
            when (val result = requestRepository.deleteRequest(busId)) {
                is ApiResult.Success -> {
                    _snackbarMessage.value = "Viaje cancelado"
                    loadLocalTrip()
                }
                is ApiResult.HttpError -> _snackbarMessage.value = result.message
                is ApiResult.NetworkError -> _snackbarMessage.value = "Sin conexión a internet"
            }
        }
    }

    fun refresh() { loadLocalTrip() }
    fun consumeSnackbar() { _snackbarMessage.value = null }
}
