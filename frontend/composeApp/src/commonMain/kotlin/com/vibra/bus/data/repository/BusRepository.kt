package com.vibra.bus.data.repository

import com.vibra.bus.data.api.BusApi
import com.vibra.bus.data.model.ArrivedResponse
import com.vibra.bus.data.model.BusCatalogResponse
import com.vibra.bus.data.model.BusDetailResponse
import com.vibra.bus.data.model.BusStopsResponse
import com.vibra.bus.data.model.BusesResponse
import com.vibra.bus.data.model.OccupancyResponse
import com.vibra.bus.util.ApiResult

class BusRepository(
    private val api: BusApi,
    private val googleMapsApi: com.vibra.bus.data.api.GoogleMapsApi? = null
) {
    suspend fun getBusCatalog(): ApiResult<BusCatalogResponse> = api.getBusCatalog()
    suspend fun getBuses(lat: Double, lng: Double): ApiResult<BusesResponse> = api.getBuses(lat, lng)
    suspend fun getBusDetail(plate: String): ApiResult<BusDetailResponse> = api.getBusDetail(plate)
    suspend fun getBusStops(plate: String): ApiResult<BusStopsResponse> = api.getBusStops(plate)
    suspend fun getBusOccupancy(plate: String): ApiResult<OccupancyResponse> = api.getBusOccupancy(plate)
    suspend fun confirmArrival(plate: String, stopId: Int): ApiResult<ArrivedResponse> = api.confirmArrival(plate, stopId)
    suspend fun notifyApproaching(plate: String, stopId: Int): ApiResult<ArrivedResponse> = api.notifyApproaching(plate, stopId)
    suspend fun updateBusOccupancy(plate: String, isFull: Boolean): ApiResult<OccupancyResponse> = api.updateBusOccupancy(plate, isFull)

    suspend fun getBusRoute(plate: String): ApiResult<com.vibra.bus.data.model.DirectionsResponse> = api.getBusRoute(plate)

    suspend fun getDirections(origin: String, destination: String, waypoints: String?, apiKey: String) =
        googleMapsApi?.getDirections(origin, destination, waypoints, apiKey)
}
