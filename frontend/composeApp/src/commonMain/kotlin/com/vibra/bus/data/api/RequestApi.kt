package com.vibra.bus.data.api

import com.vibra.bus.data.model.CreateRequestBody
import com.vibra.bus.data.model.CreateRequestResponse
import com.vibra.bus.data.model.DeleteRequestResponse
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class RequestApi(private val client: HttpClient) {

    suspend fun createRequest(busId: Int, stopId: Int): ApiResult<CreateRequestResponse> = safeCall {
        client.post("$BASE_URL/requests") {
            contentType(ContentType.Application.Json)
            setBody(CreateRequestBody(busId, stopId))
        }.body()
    }

    suspend fun deleteRequest(busId: Int): ApiResult<DeleteRequestResponse> = safeCall {
        client.delete("$BASE_URL/requests/$busId").body()
    }
}
