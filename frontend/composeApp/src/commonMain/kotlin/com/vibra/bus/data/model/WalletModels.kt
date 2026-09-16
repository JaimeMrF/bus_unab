package com.vibra.bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── GET /api/v1/wallet ─────────────────────────────────────────────

@Serializable
data class WalletTx(
    val id: Int,
    val tipo: String,                              // credit | debit | ajuste
    @SerialName("monto_centavos") val montoCentavos: Long,
    @SerialName("balance_after") val balanceAfter: Long,
    val reference: String? = null,
    val contraparte: String? = null,               // pasajero | transportadora | plataforma | mock
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class WalletInfo(
    @SerialName("wallet_id") val walletId: Int,
    @SerialName("balance_centavos") val balanceCentavos: Long,
    val estado: String = "activa",
    val version: Int = 0,
    val transactions: List<WalletTx> = emptyList(),
)

@Serializable
data class WalletResponse(
    val success: Boolean = false,
    val data: WalletInfo? = null,
    val message: String? = null,
)

// ── POST /api/v1/wallet/recharge-mock ──────────────────────────────

@Serializable
data class RechargeBody(
    @SerialName("monto_centavos") val montoCentavos: Int,
)

@Serializable
data class RechargeData(
    @SerialName("balance_centavos") val balanceCentavos: Long,
    @SerialName("transaction_id") val transactionId: Int,
    val reference: String? = null,
)

@Serializable
data class RechargeResponse(
    val success: Boolean = false,
    val data: RechargeData? = null,
    val message: String? = null,
)

// ── POST /api/v1/wallet/qr/issue ───────────────────────────────────

@Serializable
data class IssueQrBody(
    @SerialName("bus_id") val busId: Int? = null,
)

@Serializable
data class IssueQrData(
    val qr: String,                                // "selector.firma" — rota cada TTL
    @SerialName("token_id") val tokenId: Int,
    @SerialName("monto_centavos") val montoCentavos: Long,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("ttl_seconds") val ttlSeconds: Int = 60,
)

@Serializable
data class IssueQrResponse(
    val success: Boolean = false,
    val data: IssueQrData? = null,
    val message: String? = null,
)

// ── POST /api/v1/qr/pay (mPOS conductor) ───────────────────────────

@Serializable
data class PayQrBody(
    val qr: String,
)

@Serializable
data class PayQrData(
    @SerialName("token_id") val tokenId: Int,
    @SerialName("monto_centavos") val montoCentavos: Long,
    val contraparte: String? = null,
    val reference: String? = null,
    @SerialName("saldo_restante") val saldoRestante: Long,
    val pasajero: String? = null,
)

@Serializable
data class PayQrResponse(
    val success: Boolean = false,
    val data: PayQrData? = null,
    val message: String? = null,
)
