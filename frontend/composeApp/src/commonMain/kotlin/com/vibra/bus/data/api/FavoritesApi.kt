package com.vibra.bus.data.api

import kotlinx.coroutines.CancellationException
import io.ktor.http.isSuccess
import io.ktor.client.statement.HttpResponse
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

/**
 * Favoritos de paradas del usuario: GET/POST /favorites/stops y DELETE /favorites/stops/{id}.
 * El cliente Ktor no lanza en 4xx/5xx, asi que el codigo de estado se comprueba a mano: un 401,
 * 404 o 500 nunca debe tomarse por exito (el repositorio descartaria cambios pendientes).
 */
class FavoritesApi(private val client: HttpClient) {

    suspend fun list(): ApiResult<FavoriteStopsResponse> = guarded {
        val r = client.get("$BASE_URL/favorites/stops")
        if (r.status.isSuccess()) ApiResult.Success(r.body<FavoriteStopsResponse>()) else httpError(r)
    }

    suspend fun add(stopId: Int): ApiResult<Unit> = guarded {
        val r = client.post("$BASE_URL/favorites/stops") {
            contentType(ContentType.Application.Json)
            setBody(FavoriteStopBody(stopId))
        }
        if (r.status.isSuccess()) ApiResult.Success(Unit) else httpError(r)
    }

    /** Idempotente en el servidor: 204 exista o no. */
    suspend fun remove(stopId: Int): ApiResult<Unit> = guarded {
        val r = client.delete("$BASE_URL/favorites/stops/$stopId")
        if (r.status.isSuccess()) ApiResult.Success(Unit) else httpError(r)
    }

    private fun httpError(r: HttpResponse): ApiResult<Nothing> = ApiResult.HttpError(r.status.value, r.status.description)

    private suspend fun <T> guarded(block: suspend () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Network error")
        }
}
