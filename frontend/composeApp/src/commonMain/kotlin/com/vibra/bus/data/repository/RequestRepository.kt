package com.vibra.bus.data.repository

import com.vibra.bus.data.api.RequestApi
import com.vibra.bus.data.model.CreateRequestResponse
import com.vibra.bus.data.model.DeleteRequestResponse
import com.vibra.bus.data.model.ListRequestsResponse
import com.vibra.bus.data.model.QRPayload
import com.vibra.bus.data.model.QrValidateData
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings

class RequestRepository(
    private val api: RequestApi,
    private val settings: AppSettings,
) {
    suspend fun getMyRequests(): ApiResult<ListRequestsResponse> = api.getMyRequests()

    suspend fun createRequest(busId: Int, stopId: Int): ApiResult<CreateRequestResponse> {
        return api.createRequest(busId, stopId).also { result ->
            if (result is ApiResult.Success) {
                settings.activeRequestId = result.data.data?.request?.id ?: -1
                settings.activeRequestBusId = busId
                settings.activeRequestStopId = stopId
            }
        }
    }

    suspend fun deleteRequest(busId: Int): ApiResult<DeleteRequestResponse> {
        return api.deleteRequest(busId).also { result ->
            if (result is ApiResult.Success) {
                settings.activeRequestId = -1
                settings.activeRequestBusId = -1
                settings.activeRequestStopId = -1
            }
        }
    }

    fun getActiveRequest(): Triple<Int, Int, Int> = Triple(
        settings.activeRequestId,
        settings.activeRequestBusId,
        settings.activeRequestStopId,
    )

    suspend fun validateQr(payload: QRPayload): ApiResult<QrValidateData> =
        api.validateQr(payload)
}
