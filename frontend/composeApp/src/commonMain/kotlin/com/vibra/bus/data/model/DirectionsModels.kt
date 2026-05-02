package com.vibra.bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DirectionsResponse(
    val routes: List<Route> = emptyList(),
    val status: String? = null
)

/**
 * Envoltorio estándar para las respuestas de la API de VibraBus
 */
@Serializable
data class BusRouteResponse(
    val success: Boolean,
    val data: DirectionsResponse
)

@Serializable
data class Route(
    @SerialName("overview_polyline") val overviewPolyline: Polyline
)

@Serializable
data class Polyline(
    val points: String
)
