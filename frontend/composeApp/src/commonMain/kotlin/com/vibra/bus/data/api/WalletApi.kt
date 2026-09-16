package com.vibra.bus.data.api

import com.vibra.bus.data.model.IssueQrBody
import com.vibra.bus.data.model.IssueQrData
import com.vibra.bus.data.model.IssueQrResponse
import com.vibra.bus.data.model.PayQrBody
import com.vibra.bus.data.model.PayQrData
import com.vibra.bus.data.model.PayQrResponse
import com.vibra.bus.data.model.RechargeBody
import com.vibra.bus.data.model.RechargeData
import com.vibra.bus.data.model.RechargeResponse
import com.vibra.bus.data.model.WalletInfo
import com.vibra.bus.data.model.WalletResponse
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class WalletApi(private val client: HttpClient) {

    /** GET /api/v1/wallet — saldo + últimos 20 asientos del ledger. */
    suspend fun getWallet(): ApiResult<WalletInfo> = safeCall {
        val dto: WalletResponse = client.get("$BASE_URL/wallet").body()
        dto.data ?: throw Exception(dto.message ?: "Error desconocido")
    }

    /** POST /api/v1/wallet/recharge-mock — solo local/testing. */
    suspend fun rechargeMock(montoCentavos: Int): ApiResult<RechargeData> = safeCall {
        val dto: RechargeResponse = client.post("$BASE_URL/wallet/recharge-mock") {
            contentType(ContentType.Application.Json)
            setBody(RechargeBody(montoCentavos))
        }.body()
        dto.data ?: throw Exception(dto.message ?: "Recarga rechazada")
    }

    /** POST /api/v1/wallet/qr/issue — QR dinámico de pago ("selector.firma", 60s). */
    suspend fun issueQr(busId: Int? = null): ApiResult<IssueQrData> = safeCall {
        val dto: IssueQrResponse = client.post("$BASE_URL/wallet/qr/issue") {
            contentType(ContentType.Application.Json)
            setBody(IssueQrBody(busId))
        }.body()
        dto.data ?: throw Exception(dto.message ?: "No se pudo generar el QR")
    }

    /**
     * POST /api/v1/qr/pay — cobro mPOS (role admin|driver).
     * En 4xx leemos el body de la excepción para mostrar la causa real de
     * Laravel (expirado / replay / firma inválida / saldo insuficiente);
     * si el body no es legible, caemos al código de estado.
     */
    suspend fun pay(qr: String): ApiResult<PayQrData> = safeCall {
        val dto = try {
            client.post("$BASE_URL/qr/pay") {
                contentType(ContentType.Application.Json)
                setBody(PayQrBody(qr))
            }.body<PayQrResponse>()
        } catch (e: ClientRequestException) {
            runCatching { e.response.body<PayQrResponse>() }
                .getOrElse { throw Exception("HTTP ${e.response.status.value}") }
        }
        dto.data ?: throw Exception(dto.message ?: "Cobro rechazado")
    }
}
