package com.vibra.bus.data.repository

import com.vibra.bus.data.api.WalletApi
import com.vibra.bus.data.model.IssueQrData
import com.vibra.bus.data.model.PayQrData
import com.vibra.bus.data.model.RechargeData
import com.vibra.bus.data.model.WalletInfo
import com.vibra.bus.util.ApiResult

class WalletRepository(private val api: WalletApi) {

    suspend fun getWallet(): ApiResult<WalletInfo> = api.getWallet()

    suspend fun rechargeMock(montoCentavos: Int): ApiResult<RechargeData> =
        api.rechargeMock(montoCentavos)

    suspend fun issueQr(busId: Int? = null): ApiResult<IssueQrData> = api.issueQr(busId)

    suspend fun pay(qr: String): ApiResult<PayQrData> = api.pay(qr)
}
