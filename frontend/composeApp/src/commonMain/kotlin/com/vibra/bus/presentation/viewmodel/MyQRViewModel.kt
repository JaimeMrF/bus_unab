package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.QRPayload
import com.vibra.bus.util.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.TimeSource

/**
 * QR de viaje del pasajero. Vale [VALID_S] segundos desde su sello de tiempo y se renueva
 * [RENEW_MARGIN_S] segundos antes de expirar, de modo que nunca se muestra uno vencido.
 * La cuenta regresiva usa reloj monotónico (no depende de sumar delays ni de cambios de hora).
 */
class MyQRViewModel(private val settings: AppSettings) : ViewModel() {

    private val _qrContent = MutableStateFlow("")
    val qrContent: StateFlow<String> = _qrContent

    private val _countdown = MutableStateFlow(VALID_S)
    val countdown: StateFlow<Int> = _countdown

    private var rotationJob: Job? = null

    init {
        startRotation()
    }

    /** Fuerza un QR nuevo ahora (p. ej. botón de regenerar). */
    fun generateQR() = startRotation()

    private fun startRotation() {
        rotationJob?.cancel()
        rotationJob = viewModelScope.launch {
            while (true) {
                // La serialización va fuera del hilo principal; el sello de tiempo se toma al emitir.
                val content = withContext(Dispatchers.Default) {
                    Json.encodeToString(
                        QRPayload(
                            requestId = settings.activeRequestId,
                            userId = settings.userId,
                            busId = settings.activeRequestBusId,
                            stopId = settings.activeRequestStopId,
                            ts = Clock.System.now().toEpochMilliseconds(),
                        )
                    )
                }
                _qrContent.value = content
                val issued = TimeSource.Monotonic.markNow()
                var remaining = VALID_S
                _countdown.value = remaining
                while (remaining > RENEW_MARGIN_S) {
                    delay(250)
                    remaining = VALID_S - issued.elapsedNow().inWholeSeconds.toInt()
                    if (remaining != _countdown.value) _countdown.value = remaining.coerceAtLeast(0)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        rotationJob?.cancel()
    }

    companion object {
        const val VALID_S = 60
        const val RENEW_MARGIN_S = 5
    }
}
