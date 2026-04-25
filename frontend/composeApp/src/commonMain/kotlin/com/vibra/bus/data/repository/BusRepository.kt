package com.vibra.bus.data.repository

import com.vibra.bus.data.api.BusApi
import com.vibra.bus.data.model.ArrivedResponse
import com.vibra.bus.data.model.BusDetailResponse
import com.vibra.bus.data.model.BusStopsResponse
import com.vibra.bus.data.model.BusesResponse
import com.vibra.bus.data.model.OccupancyResponse
import com.vibra.bus.util.ApiResult

class BusRepository(private val api: BusApi) {
    suspend fun getBuses(lat: Double, lng: Double): ApiResult<BusesResponse> = api.getBuses(lat, lng)
    suspend fun getBusDetail(plate: String): ApiResult<BusDetailResponse> = api.getBusDetail(plate)
    suspend fun getBusStops(plate: String): ApiResult<BusStopsResponse> = api.getBusStops(plate)
    suspend fun getBusOccupancy(plate: String): ApiResult<OccupancyResponse> = api.getBusOccupancy(plate)
    suspend fun confirmArrival(plate: String, stopId: Int): ApiResult<ArrivedResponse> = api.confirmArrival(plate, stopId)
}
