package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.QRPayload
import com.vibra.bus.data.model.QrValidateData
import com.vibra.bus.data.repository.RequestRepository
import com.vibra.bus.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

sealed class ScanState {
    data object Scanning : ScanState()
    data object Validating : ScanState()
    data class Valid(val data: QrValidateData) : ScanState()
    data object Expired : ScanState()
    data object Invalid : ScanState()
    data class Error(val message: String) : ScanState()
}

class QRScannerViewModel(
    private val repository: RequestRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private var lastScanMs = 0L

    fun onQrScanned(content: String) {
        val currentState = _state.value
        if (currentState is ScanState.Validating) return

        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastScanMs < 2_000 && currentState !is ScanState.Scanning) return

        val payload = try {
            Json.decodeFromString<QRPayload>(content)
        } catch (e: Exception) {
            _state.value = ScanState.Invalid
            return
        }

        if (now - payload.ts > 60_000) {
            _state.value = ScanState.Expired
            return
        }

        lastScanMs = now
        viewModelScope.launch {
            _state.value = ScanState.Validating
            _state.value = when (val result = repository.validateQr(payload)) {
                is ApiResult.Success    -> ScanState.Valid(result.data)
                is ApiResult.HttpError  -> ScanState.Error(result.message)
                is ApiResult.NetworkError -> ScanState.Error(result.message)
            }
        }
    }

    fun reset() {
        lastScanMs = 0L
        _state.value = ScanState.Scanning
    }
}
