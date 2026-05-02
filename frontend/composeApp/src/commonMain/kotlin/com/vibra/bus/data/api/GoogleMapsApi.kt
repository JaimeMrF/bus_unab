package com.vibra.bus.data.api

import com.vibra.bus.data.model.DirectionsResponse
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class GoogleMapsApi(private val client: HttpClient) {

    // IMPORTANT: In a production app, you should proxy this through your backend
    // to keep your API Key secure. For this exercise, we'll call it directly.
    suspend fun getDirections(
        origin: String,
        destination: String,
        waypoints: String?,
        apiKey: String
    ): ApiResult<DirectionsResponse> = safeCall {
        client.get("https://maps.googleapis.com/maps/api/directions/json") {
            parameter("origin", origin)
            parameter("destination", destination)
            if (waypoints != null) {
                parameter("waypoints", waypoints)
            }
            parameter("key", apiKey)
            parameter("mode", "driving")
        }.body()
    }
}
