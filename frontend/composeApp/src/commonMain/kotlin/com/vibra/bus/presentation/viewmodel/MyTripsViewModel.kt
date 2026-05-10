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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    init {
        loadTrips()
    }

    fun loadTrips() {
        viewModelScope.launch {
            _tripsState.value = UiState.Loading
            when (val result = requestRepository.getMyRequests()) {
                is ApiResult.Success -> {
                    _tripsState.value = UiState.Success(result.data.data)
                }
                is ApiResult.HttpError -> {
                    _tripsState.value = UiState.Error(result.message)
                }
                is ApiResult.NetworkError -> {
                    _tripsState.value = UiState.Error("Sin conexión a internet")
                }
            }
        }
    }

    fun cancelTrip(busId: Int) {
        viewModelScope.launch {
            when (val result = requestRepository.deleteRequest(busId)) {
                is ApiResult.Success -> {
                    _snackbarMessage.value = "Viaje cancelado"
                    loadTrips()
                }
                is ApiResult.HttpError -> _snackbarMessage.value = result.message
                is ApiResult.NetworkError -> _snackbarMessage.value = "Sin conexión a internet"
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadTrips()
            _isRefreshing.value = false
        }
    }

    fun consumeSnackbar() { _snackbarMessage.value = null }
}
