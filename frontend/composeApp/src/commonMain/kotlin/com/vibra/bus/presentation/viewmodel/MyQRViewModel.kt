package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.QRPayload
import com.vibra.bus.util.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MyQRViewModel(private val settings: AppSettings) : ViewModel() {

    private val _qrContent = MutableStateFlow("")
    val qrContent: StateFlow<String> = _qrContent

    private val _countdown = MutableStateFlow(60)
    val countdown: StateFlow<Int> = _countdown

    private var countdownJob: Job? = null

    init {
        generateQR()
    }

    fun generateQR() {
        val payload = QRPayload(
            requestId = settings.activeRequestId,
            userId = settings.userId,
            busId = settings.activeRequestBusId,
            stopId = settings.activeRequestStopId,
            ts = Clock.System.now().toEpochMilliseconds(),
        )
        _qrContent.value = Json.encodeToString(payload)
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        _countdown.value = 60
        countdownJob = viewModelScope.launch {
            while (_countdown.value > 0) {
                delay(1_000)
                _countdown.value -= 1
            }
            generateQR()
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
