package com.vibra.bus.presentation.viewmodel

import kotlin.time.TimeSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.bus.data.model.IssueQrData
import com.vibra.bus.data.model.WalletInfo
import com.vibra.bus.data.model.WalletTx
import com.vibra.bus.data.repository.WalletRepository
import com.vibra.bus.util.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Formatea centavos COP → "$ 4.350" (sin decimales, separador de miles). */
fun formatCentavosCop(centavos: Long): String {
    val pesos = centavos / 100
    val s = pesos.toString()
    val grouped = buildString {
        s.reversed().forEachIndexed { i, c ->
            if (i > 0 && i % 3 == 0) append('.')
            append(c)
        }
    }.reversed()
    return "$ $grouped"
}

class WalletViewModel(
    private val repository: WalletRepository,
) : ViewModel() {

    private val _wallet = MutableStateFlow<WalletInfo?>(null)
    val wallet: StateFlow<WalletInfo?> = _wallet.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Mensaje transitorio (error o confirmación); el screen lo limpia. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ── QR de pago dinámico (rota cada ttl_seconds del server) ──────
    private val _payQr = MutableStateFlow<IssueQrData?>(null)
    val payQr: StateFlow<IssueQrData?> = _payQr.asStateFlow()

    private val _payCountdown = MutableStateFlow(0)
    val payCountdown: StateFlow<Int> = _payCountdown.asStateFlow()

    private var payQrJob: Job? = null
    private var payVisible = false

    fun consumeMessage() { _message.value = null }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            when (val r = repository.getWallet()) {
                is ApiResult.Success   -> _wallet.value = r.data
                is ApiResult.HttpError -> _message.value = r.message
                is ApiResult.NetworkError -> _message.value = r.message
            }
            _loading.value = false
        }
    }

    /** Recarga mock (solo local/testing). montoCentavos ≤ 500.000 (tope server). */
    fun recharge(montoCentavos: Int) {
        viewModelScope.launch {
            _loading.value = true
            val r = repository.rechargeMock(montoCentavos)
            when {
                r is ApiResult.Success -> {
                    _message.value = "Recarga aplicada ✔"
                    refresh()
                }
                else -> {
                    _loading.value = false
                    _message.value = when (r) {
                        is ApiResult.HttpError    -> r.message
                        is ApiResult.NetworkError -> r.message
                        else -> "Recarga fallida"
                    }
                }
            }
            _loading.value = false
        }
    }

    // ── QR de pago ──────────────────────────────────────────────────

    fun showPayQr(busId: Int? = null) {
        if (payVisible) return
        payVisible = true
        startPayRotation(busId)
    }

    fun hidePayQr() {
        payVisible = false
        payQrJob?.cancel()
        payQrJob = null
        _payQr.value = null
        _payCountdown.value = 0
    }

    fun regeneratePayQr(busId: Int? = null) {
        payQrJob?.cancel()
        startPayRotation(busId)
    }

    /**
     * Rota el QR: pide uno nuevo cuando faltan [RENEW_MARGIN_S] segundos para expirar (el viejo
     * sigue visible hasta que llega el nuevo). El tiempo restante se mide con reloj monotónico,
     * no sumando delays, así que no se desfasa si la app se pausa o el reloj del equipo cambia.
     * Si emitir falla se reintenta con espera exponencial (5, 10, 20... máx. 60 s).
     */
    private fun startPayRotation(busId: Int?) {
        payQrJob?.cancel()
        payQrJob = viewModelScope.launch {
            var failures = 0
            while (payVisible) {
                when (val r = repository.issueQr(busId)) {
                    is ApiResult.Success -> {
                        failures = 0
                        _payQr.value = r.data
                        val ttl = r.data.ttlSeconds.coerceAtLeast(RENEW_MARGIN_S + 5)
                        val issued = TimeSource.Monotonic.markNow()
                        var remaining = ttl
                        _payCountdown.value = remaining
                        while (payVisible && remaining > RENEW_MARGIN_S) {
                            delay(250)
                            remaining = ttl - issued.elapsedNow().inWholeSeconds.toInt()
                            if (remaining != _payCountdown.value) _payCountdown.value = remaining.coerceAtLeast(0)
                        }
                    }
                    else -> {
                        failures++
                        // Con un QR vigente se mantiene visible; solo se limpia si ya expiró.
                        if (_payCountdown.value <= 0) _payQr.value = null
                        _message.value = when (r) {
                            is ApiResult.HttpError    -> r.message
                            is ApiResult.NetworkError -> "Sin conexión. Reintentando…"
                            else -> "No se pudo generar el QR"
                        }
                        delay(minOf(60_000L, 5_000L shl (failures - 1).coerceAtMost(4)))
                    }
                }
            }
        }
    }

    private companion object {
        const val RENEW_MARGIN_S = 5
    }

    val transactions: List<WalletTx>
        get() = _wallet.value?.transactions ?: emptyList()

    override fun onCleared() {
        super.onCleared()
        payQrJob?.cancel()
    }
}
