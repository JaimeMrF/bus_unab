package com.vibra.bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val role: String = "student",
)

@Serializable
data class AuthResponseData(
    val user: UserDto,
    @SerialName("access_token") val token: String,
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val data: AuthResponseData? = null,
)

@Serializable
data class MeResponse(
    val success: Boolean,
    val data: UserDto,
)

@Serializable
data class GoogleTokenRequest(
    @SerialName("id_token") val idToken: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class DeviceTokenRequest(
    val token: String,
    val platform: String,
)

@Serializable
data class BasicResponse(
    val success: Boolean,
    val message: String? = null,
)
