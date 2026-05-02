package com.vibra.bus.data.api

import com.vibra.bus.data.model.ArrivedRequest
import com.vibra.bus.data.model.ArrivedResponse
import com.vibra.bus.data.model.BusDetailResponse
import com.vibra.bus.data.model.BusStopsResponse
import com.vibra.bus.data.model.BusesResponse
import com.vibra.bus.data.model.OccupancyResponse
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class BusApi(private val client: HttpClient) {

    suspend fun getBuses(lat: Double, lng: Double): ApiResult<BusesResponse> = safeCall {
        client.get("$BASE_URL/buses") {
            parameter("lat", lat)
            parameter("lng", lng)
        }.body()
    }

    suspend fun getBusDetail(plate: String): ApiResult<BusDetailResponse> = safeCall {
        client.get("$BASE_URL/buses/$plate").body()
    }

    suspend fun getBusStops(plate: String): ApiResult<BusStopsResponse> = safeCall {
        client.get("$BASE_URL/buses/$plate/stops").body()
    }

    suspend fun getBusOccupancy(plate: String): ApiResult<OccupancyResponse> = safeCall {
        client.get("$BASE_URL/buses/$plate/occupancy").body()
    }

    suspend fun confirmArrival(plate: String, stopId: Int): ApiResult<ArrivedResponse> = safeCall {
        client.post("$BASE_URL/buses/$plate/arrived") {
            contentType(ContentType.Application.Json)
            setBody(ArrivedRequest(stopId))
        }.body()
    }

    suspend fun updateBusOccupancy(plate: String, isFull: Boolean): ApiResult<OccupancyResponse> = safeCall {
        client.post("$BASE_URL/buses/$plate/occupancy") {
            contentType(ContentType.Application.Json)
            setBody(com.vibra.bus.data.model.OccupancyRequest(isFull))
        }.body()
    }

    suspend fun getBusRoute(plate: String): ApiResult<com.vibra.bus.data.model.DirectionsResponse> = safeCall {
        client.get("$BASE_URL/buses/$plate/route").body()
    }
}
