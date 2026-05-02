package com.vibra.bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StopDto(
    val id: Int,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("radius_meters") val radiusMeters: Int = 50,
)

@Serializable
data class StopPivot(
    val order: Int = 0,
    @SerialName("estimated_minutes") val estimatedMinutes: Int = 0,
)

@Serializable
data class StopWithPivotDto(
    val id: Int,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("radius_meters") val radiusMeters: Int = 50,
    val order: Int = 0,
    @SerialName("estimated_minutes") val estimatedMinutes: Int = 0,
    val pivot: StopPivot? = null,
)

@Serializable
data class StopsResponse(
    val success: Boolean,
    val data: List<StopDto> = emptyList(),
)

@Serializable
data class BusStopsResponse(
    val success: Boolean,
    val data: List<StopWithPivotDto> = emptyList(),
)
