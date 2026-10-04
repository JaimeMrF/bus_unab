package com.vibra.bus.data.api

import com.vibra.bus.data.model.StopsResponse
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient

class StopApi(private val client: HttpClient) {

    suspend fun getStops(): ApiResult<StopsResponse> = safeCall {
        client.getConditional<StopsResponse>("$BASE_URL/stops")
    }
}
