package com.vibra.bus.util

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Long.toRelativeTime(): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - this
    return when {
        diff < 60_000 -> "Hace un momento"
        diff < 3_600_000 -> "Hace ${diff / 60_000} min"
        diff < 86_400_000 -> "Hace ${diff / 3_600_000} h"
        else -> "Hace ${diff / 86_400_000} días"
    }
}

fun String.toDisplayTime(): String {
    return try {
        val instant = Instant.parse(this)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        this
    }
}

fun String.occupancyLevelToColor(): Color = when (this) {
    "low"    -> Color(0xFF36C416)
    "medium" -> Color(0xFFE88B11)
    "high"   -> Color(0xFFF97316)
    "full"   -> Color(0xFFEF4444)
    else     -> Color(0xFF71717B)
}
