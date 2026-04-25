package com.vibra.bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusSummaryDto(
    val id: Int,
    val name: String,
    val plate: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Int = 0,
)

@Serializable
data class BusDetailDto(
    val id: Int,
    val name: String,
    val plate: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("speed_kmh") val speedKmh: Int = 0,
    val heading: Int = 0,
    val address: String? = null,
    val driver: String? = null,
    @SerialName("last_event") val lastEvent: String? = null,
    @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
)

@Serializable
data class BusesResponse(
    val success: Boolean,
    val data: List<BusSummaryDto> = emptyList(),
)

@Serializable
data class BusDetailResponse(
    val success: Boolean,
    val data: BusDetailDto? = null,
)

@Serializable
data class OccupancyDto(
    @SerialName("bus_id") val busId: Int,
    @SerialName("bus_name") val busName: String,
    @SerialName("current_occupancy") val currentOccupancy: Int,
    val capacity: Int,
    val percentage: Float,
    val level: String,
)

@Serializable
data class OccupancyResponse(
    val success: Boolean,
    val data: OccupancyDto? = null,
)

@Serializable
data class ArrivedRequest(
    @SerialName("stop_id") val stopId: Int,
)

@Serializable
data class ArrivedResponseData(
    @SerialName("notified_users") val notifiedUsers: Int,
)

@Serializable
data class ArrivedResponse(
    val success: Boolean,
    val data: ArrivedResponseData,
)
