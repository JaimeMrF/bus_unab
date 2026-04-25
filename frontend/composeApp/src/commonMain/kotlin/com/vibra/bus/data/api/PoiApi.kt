package com.vibra.bus.data.api

import com.vibra.bus.data.model.PoisResponse
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PoiApi(private val client: HttpClient) {

    suspend fun getPois(lat: Double, lng: Double, category: String? = null): ApiResult<PoisResponse> = safeCall {
        client.get("$BASE_URL/poi") {
            parameter("lat", lat)
            parameter("lng", lng)
            category?.let { parameter("category", it) }
        }.body()
    }
}
