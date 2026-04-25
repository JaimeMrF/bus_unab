package com.vibra.bus.data.api

import com.vibra.bus.data.model.StopsResponse
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class StopApi(private val client: HttpClient) {

    suspend fun getStops(): ApiResult<StopsResponse> = safeCall {
        client.get("$BASE_URL/stops").body()
    }
}
