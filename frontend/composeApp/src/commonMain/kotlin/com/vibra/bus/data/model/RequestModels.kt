package com.vibra.bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateRequestBody(
    @SerialName("bus_id") val busId: Int,
    @SerialName("stop_id") val stopId: Int,
)

@Serializable
data class RequestBusInfo(
    val id: Int,
    val name: String,
    val plate: String,
)

@Serializable
data class RequestStopInfo(
    val id: Int,
    val name: String,
    val address: String,
)

@Serializable
data class RequestInfo(
    val id: Int,
    val status: String,
    val bus: RequestBusInfo,
    val stop: RequestStopInfo,
)

@Serializable
data class CreateRequestData(
    val request: RequestInfo,
    @SerialName("current_occupancy") val currentOccupancy: Int,
    val capacity: Int,
    val percentage: Float,
    val level: String,
    @SerialName("is_full") val isFull: Boolean,
)

@Serializable
data class CreateRequestResponse(
    val success: Boolean,
    val data: CreateRequestData? = null,
)

@Serializable
data class DeleteRequestResponse(
    val success: Boolean,
)

@Serializable
data class QRPayload(
    @SerialName("request_id") val requestId: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("bus_id") val busId: Int,
    @SerialName("stop_id") val stopId: Int,
    val ts: Long,
)

@Serializable
data class QrValidateRequest(
    @SerialName("request_id") val requestId: Int,
    @SerialName("user_id")    val userId: Int,
    @SerialName("bus_id")     val busId: Int,
    @SerialName("stop_id")    val stopId: Int,
    val ts: Long,
)

@Serializable
data class QrValidateUserInfo(
    val id: Int,
    val name: String,
    val email: String,
)

@Serializable
data class QrValidateData(
    val user: QrValidateUserInfo,
    val bus: String,
    val stop: String,
)

@Serializable
data class QrValidateResponse(
    val success: Boolean,
    val data: QrValidateData? = null,
    val message: String? = null,
)
