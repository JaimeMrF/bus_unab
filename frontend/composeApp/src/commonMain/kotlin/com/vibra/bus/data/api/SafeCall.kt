package com.vibra.bus.data.api

import com.vibra.bus.util.ApiResult
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.serialization.SerializationException

suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: ClientRequestException) {
        ApiResult.HttpError(e.response.status.value, e.message ?: "Client error")
    } catch (e: ServerResponseException) {
        ApiResult.HttpError(e.response.status.value, e.message ?: "Server error")
    } catch (e: SerializationException) {
        ApiResult.NetworkError("Error de datos del servidor: ${e.message?.take(120)}")
    } catch (e: Exception) {
        ApiResult.NetworkError(e.message ?: "Network error")
    }
}
