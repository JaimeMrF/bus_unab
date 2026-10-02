package com.vibra.bus.data.api

import com.vibra.bus.domain.brand.BrandConfig
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

@Serializable
data class BrandResponse(val success: Boolean = true, val data: BrandConfig? = null,
    /** Solo cliente: código HTTP cuando la respuesta no fue 2xx. */
    val status: Int = 200,
)

/** Endpoint público (sin auth): el cliente todavía no conoce el tenant. */
class BrandApi(private val client: HttpClient) {

    suspend fun getBranding(slug: String): ApiResult<BrandResponse> = safeCall {
        val response = client.get("$BASE_URL/branding/$slug")
        if (!response.status.isSuccess()) {
            return@safeCall BrandResponse(success = false, data = null, status = response.status.value)
        }
        response.body<BrandResponse>()
    }
}
