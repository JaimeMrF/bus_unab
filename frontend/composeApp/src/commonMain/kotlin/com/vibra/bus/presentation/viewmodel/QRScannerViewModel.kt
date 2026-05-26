package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.QRPayload
import com.vibra.bus.data.model.QrValidateData
import com.vibra.bus.data.repository.RequestRepository
import com.vibra.bus.util.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

sealed class ScanState {
    data object Scanning : ScanState()
    data object Validating : ScanState()
}

sealed class ScanResult {
    data class Valid(val data: QrValidateData) : ScanResult()
    data object Expired : ScanResult()
    data object Invalid : ScanResult()
    data class Error(val message: String) : ScanResult()
}

class QRScannerViewModel(
    private val repository: RequestRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _lastResult = MutableStateFlow<ScanResult?>(null)
    val lastResult: StateFlow<ScanResult?> = _lastResult.asStateFlow()

    private var lastScanMs = 0L
    private var clearResultJob: Job? = null

    fun onQrScanned(content: String) {
        if (_state.value is ScanState.Validating) return

        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastScanMs < 2_500) return

        val payload = try {
            Json.decodeFromString<QRPayload>(content)
        } catch (e: Exception) {
            showResult(ScanResult.Invalid)
            return
        }

        if (now - payload.ts > 60_000) {
            showResult(ScanResult.Expired)
            return
        }

        lastScanMs = now
        viewModelScope.launch {
            _state.value = ScanState.Validating
            val result = when (val r = repository.validateQr(payload)) {
                is ApiResult.Success      -> ScanResult.Valid(r.data)
                is ApiResult.HttpError    -> ScanResult.Error(r.message)
                is ApiResult.NetworkError -> ScanResult.Error(r.message)
            }
            _state.value = ScanState.Scanning
            showResult(result)
        }
    }

    private fun showResult(result: ScanResult) {
        clearResultJob?.cancel()
        _lastResult.value = result
        lastScanMs = Clock.System.now().toEpochMilliseconds()
        clearResultJob = viewModelScope.launch {
            delay(3_500)
            _lastResult.value = null
        }
    }
}
