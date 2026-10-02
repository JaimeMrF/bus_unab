package com.vibra.bus.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.PayQrData
import com.vibra.bus.data.model.QRPayload
import com.vibra.bus.data.model.QrValidateData
import com.vibra.bus.data.repository.RequestRepository
import com.vibra.bus.data.repository.WalletRepository
import com.vibra.bus.util.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/** ACCESS = QR de viaje (valida reserva, legacy). PAY = mPOS (cobra wallet con QR dinámico). */
enum class ScanMode { ACCESS, PAY }

sealed class ScanState {
    data object Scanning : ScanState()
    data object Validating : ScanState()
}

sealed class ScanResult {
    data class Valid(val data: QrValidateData) : ScanResult()
    data class Charged(val data: PayQrData) : ScanResult()
    data object Expired : ScanResult()
    data object Invalid : ScanResult()
    data class Error(val message: String) : ScanResult()

    /** Sin red: el cobro NO se confirmó. Se conserva el QR para reintentar con la misma clave. */
    data class Offline(val message: String) : ScanResult()
}

class QRScannerViewModel(
    private val repository: RequestRepository,
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val _mode = MutableStateFlow(ScanMode.ACCESS)
    val mode: StateFlow<ScanMode> = _mode.asStateFlow()

    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _lastResult = MutableStateFlow<ScanResult?>(null)
    val lastResult: StateFlow<ScanResult?> = _lastResult.asStateFlow()

    /** Código escrito a mano por el conductor (fallback sin cámara). */
    private val _manualCode = MutableStateFlow("")
    val manualCode: StateFlow<String> = _manualCode.asStateFlow()

    private var lastScanMs = 0L
    private var clearResultJob: Job? = null

    /** Último contenido leído y cuándo: la cámara repite el mismo QR muchas veces por segundo. */
    private var lastContent = ""
    private var lastContentMs = 0L

    /** QR pendiente de reintento tras un corte de red (cobro o validación). */
    private var pendingPayQr: String? = null
    private var pendingPayload: QRPayload? = null

    fun setMode(mode: ScanMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        _lastResult.value = null
        clearResultJob?.cancel()
        _manualCode.value = ""
    }

    fun onManualCodeChange(value: String) { _manualCode.value = value }

    fun onManualSubmit() {
        val code = _manualCode.value.trim()
        if (code.isEmpty() || _state.value is ScanState.Validating) return
        submitQr(code, fromManual = true)
    }

    /** Descarta el resultado visible y cualquier reintento pendiente (cancelar). */
    fun dismissResult() {
        clearResultJob?.cancel()
        _lastResult.value = null
        pendingPayQr = null
        pendingPayload = null
    }

    /** Reintenta el último cobro/validación que falló por red, con la misma clave de idempotencia. */
    fun retryPending() {
        if (_state.value is ScanState.Validating) return
        pendingPayQr?.let { submitQr(it, fromManual = false); return }
        pendingPayload?.let { validateAccess(it) }
    }

    fun onQrScanned(content: String) {
        if (_state.value is ScanState.Validating) return

        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastScanMs < 2_500) return
        // Anti doble lectura: el mismo código no se procesa de nuevo durante 10 s.
        if (content == lastContent && now - lastContentMs < 10_000) return
        lastContent = content
        lastContentMs = now

        if (_mode.value == ScanMode.PAY) {
            // QR de pago = "selector.firma" crudo — nunca JSON de QRPayload
            if (!PAY_QR_REGEX.matches(content.trim())) {
                showResult(ScanResult.Invalid)
                return
            }
            submitQr(content.trim(), fromManual = false)
            return
        }

        val payload = try {
            Json.decodeFromString<QRPayload>(content)
        } catch (e: Exception) {
            showResult(ScanResult.Invalid)
            return
        }

        // Tolerancia de reloj entre dispositivos: 15 s extra de vigencia y 30 s de adelanto.
        if (payload.ts - now > 30_000) {
            showResult(ScanResult.Error("El reloj del teléfono del pasajero está desfasado"))
            return
        }
        if (now - payload.ts > 75_000) {
            showResult(ScanResult.Expired)
            return
        }

        lastScanMs = now
        validateAccess(payload)
    }

    private fun validateAccess(payload: QRPayload) {
        viewModelScope.launch {
            _state.value = ScanState.Validating
            val result = when (val r = repository.validateQr(payload)) {
                is ApiResult.Success      -> ScanResult.Valid(r.data)
                is ApiResult.HttpError    -> ScanResult.Error(r.message)
                is ApiResult.NetworkError -> ScanResult.Offline("Sin conexión. No se validó el acceso.")
            }
            _state.value = ScanState.Scanning
            pendingPayload = if (result is ScanResult.Offline) payload else null
            showResult(result)
        }
    }

    private fun submitQr(qr: String, fromManual: Boolean) {
        lastScanMs = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            _state.value = ScanState.Validating
            val result = when (val r = walletRepository.pay(qr)) {
                is ApiResult.Success -> ScanResult.Charged(r.data)
                is ApiResult.HttpError -> ScanResult.Error(r.message)
                is ApiResult.NetworkError -> ScanResult.Offline("Sin conexión. El cobro no se confirmó: reintenta con el mismo QR.")
            }
            _state.value = ScanState.Scanning
            // Solo un corte de red deja el QR pendiente; cualquier otro resultado es definitivo.
            pendingPayQr = if (result is ScanResult.Offline) qr else null
            showResult(result)
            if (fromManual && result is ScanResult.Charged) _manualCode.value = ""
        }
    }

    private fun showResult(result: ScanResult) {
        clearResultJob?.cancel()
        _lastResult.value = result
        lastScanMs = Clock.System.now().toEpochMilliseconds()
        // El aviso de "sin red" permanece hasta reintentar o cancelar.
        if (result is ScanResult.Offline) return
        clearResultJob = viewModelScope.launch {
            delay(3_500)
            _lastResult.value = null
        }
    }

    private companion object {
        /** Mismo contrato que QrPaymentService::parse (selector 32 hex + firma 64 hex). */
        val PAY_QR_REGEX = Regex("^[0-9a-fA-F]{32}\\.[0-9a-fA-F]{64}$")
    }

    override fun onCleared() {
        super.onCleared()
        clearResultJob?.cancel()
    }
}
