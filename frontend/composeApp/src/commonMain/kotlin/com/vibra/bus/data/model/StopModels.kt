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
    /** Presente solo con GET /buses/{plate}/stops?eta=1: ETA en vivo hasta esta parada. */
    @SerialName("eta_seconds") val etaSeconds: Int? = null,
) {
    /** Minutos a mostrar: ETA en vivo si existe (redondeo hacia arriba, minimo 1); si no, el estimado fijo. */
    val displayMinutes: Int
        get() = etaSeconds?.let { maxOf(1, (it + 59) / 60) } ?: estimatedMinutes
}

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
