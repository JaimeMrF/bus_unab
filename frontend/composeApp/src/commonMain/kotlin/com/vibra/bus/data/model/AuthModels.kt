package com.vibra.bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val avatar: String? = null,
    val role: String = "pasajero",
    /** Opcional hasta que el backend lo publique; si difiere del slug activo se adopta. */
    @SerialName("organization_slug") val organizationSlug: String? = null,
)

@Serializable
data class AuthResponseData(
    val user: UserDto,
    @SerialName("access_token") val token: String,
    @SerialName("organization_slug") val organizationSlug: String? = null,
)

@Serializable
data class AuthResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: AuthResponseData? = null,
)

@Serializable
data class MeResponse(
    val success: Boolean = false,
    val data: UserDto? = null,
)

@Serializable
data class GoogleTokenRequest(
    @SerialName("id_token") val idToken: String,
    /** Slug de la organizacion activa; null si el usuario aun no eligio una. */
    val organization: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    /** Un token por dispositivo: iniciar sesion aqui no cierra la sesion de otros equipos. */
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class DeviceTokenRequest(
    val token: String,
    val platform: String,
)

@Serializable
data class BasicResponse(
    val success: Boolean = false,
    val message: String? = null,
)
