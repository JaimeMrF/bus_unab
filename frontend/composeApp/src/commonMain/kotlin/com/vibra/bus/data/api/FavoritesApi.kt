package com.vibra.bus.data.api

import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Parada favorita tal como la devuelve la API; solo se usa el id (el resto es opcional). */
@Serializable
data class FavoriteStopDto(val id: Int, val name: String? = null)

@Serializable
data class FavoriteStopsResponse(
    val success: Boolean = false,
    val data: List<FavoriteStopDto>? = null,
)

@Serializable
private data class FavoriteStopBody(@SerialName("stop_id") val stopId: Int)

/** Favoritos de paradas del usuario: GET/POST /favorites/stops y DELETE /favorites/stops/{id}. */
class FavoritesApi(private val client: HttpClient) {

    suspend fun list(): ApiResult<FavoriteStopsResponse> = safeCall {
        client.get("$BASE_URL/favorites/stops").body()
    }

    suspend fun add(stopId: Int): ApiResult<Unit> = safeCall {
        client.post("$BASE_URL/favorites/stops") {
            contentType(ContentType.Application.Json)
            setBody(FavoriteStopBody(stopId))
        }
        Unit
    }

    /** Idempotente en el servidor: 204 exista o no. */
    suspend fun remove(stopId: Int): ApiResult<Unit> = safeCall {
        client.delete("$BASE_URL/favorites/stops/$stopId")
        Unit
    }
}
