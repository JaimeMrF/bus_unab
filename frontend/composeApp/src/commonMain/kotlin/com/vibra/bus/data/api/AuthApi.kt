package com.vibra.bus.data.api

import com.vibra.bus.data.model.AuthResponse
import com.vibra.bus.data.model.BasicResponse
import com.vibra.bus.data.model.DeviceTokenRequest
import com.vibra.bus.data.model.GoogleTokenRequest
import com.vibra.bus.data.model.LoginRequest
import com.vibra.bus.data.model.MeResponse
import com.vibra.bus.util.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(private val client: HttpClient) {

    suspend fun login(request: LoginRequest): ApiResult<AuthResponse> = safeCall {
        client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun loginWithGoogle(idToken: String): ApiResult<AuthResponse> = safeCall {
        client.post("$BASE_URL/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleTokenRequest(idToken))
        }.body()
    }

    suspend fun logout(): ApiResult<BasicResponse> = safeCall {
        client.post("$BASE_URL/auth/logout").body()
    }

    suspend fun getMe(): ApiResult<MeResponse> = safeCall {
        client.get("$BASE_URL/auth/me").body()
    }

    suspend fun registerDeviceToken(token: String): ApiResult<BasicResponse> = safeCall {
        client.post("$BASE_URL/device-token") {
            contentType(ContentType.Application.Json)
            setBody(DeviceTokenRequest(token))
        }.body()
    }

    suspend fun deleteDeviceToken(token: String): ApiResult<BasicResponse> = safeCall {
        client.delete("$BASE_URL/device-token") {
            contentType(ContentType.Application.Json)
            setBody(DeviceTokenRequest(token))
        }.body()
    }
}
