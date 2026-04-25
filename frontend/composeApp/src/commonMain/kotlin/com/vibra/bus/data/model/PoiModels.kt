package com.vibra.bus.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PoiDto(
    val id: Int,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class PoisResponse(
    val success: Boolean,
    val data: List<PoiDto> = emptyList(),
)

@Serializable
data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
)
