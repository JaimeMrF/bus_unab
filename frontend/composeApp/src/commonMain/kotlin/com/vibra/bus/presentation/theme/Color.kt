package com.vibra.bus.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors
val UnabPurple = Color(0xFF1D1B31)      // Deep Dark Purple
val UnabPurpleLight = Color(0xFF5B2C8C) // Standard UNAB Purple
val UnabOrange = Color(0xFFE9A427)      // UNAB Orange

val LightColorScheme = lightColorScheme(
    primary = UnabPurpleLight,
    onPrimary = Color.White,
    primaryContainer = UnabPurple,
    onPrimaryContainer = Color.White,
    secondary = UnabOrange,
    onSecondary = UnabPurple,
    background = UnabPurple,
    onBackground = Color.White,
    surface = UnabPurple,
    onSurface = Color.White,
    error = Color(0xFFE5484D),
    onError = Color.White,
)

val DarkColorScheme = darkColorScheme(
    primary = UnabPurpleLight,
    onPrimary = Color.White,
    secondary = UnabOrange,
    background = UnabPurple,
    surface = UnabPurple,
)

object GlassColors {
    val Surface = Color(0x1AFFFFFF)
    val Border = Color(0x33FFFFFF)
    val Highlight = Color(0x4DFFFFFF)
    val SurfaceDark = Color(0xB31D1B31)   // 70% UNAB Purple (Dark Glass)
    val BorderDark = Color(0x33FFFFFF)    // 20% White Border
    val HighlightDark = Color(0x1AFFFFFF) // 10% White Highlight
}

object TransportColors {
    val Success = Color(0xFF2EBE6C)
    val Warning = Color(0xFFFFB020)
    val BusAvailable = Color(0xFF2EBE6C)
    val BusFull = Color(0xFFE5484D)
    val BusApproaching = Color(0xFFE9A427)
    val RouteActive = Color(0xFFE9A427)
    val RouteInactive = Color(0xFF94A3B8)
    val OccupancyLow = Color(0xFF2EBE6C)
    val OccupancyMedium = Color(0xFFE9A427)
    val OccupancyHigh = Color(0xFFF59E0B)
}
