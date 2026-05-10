package com.vibra.bus.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors
val UnabPurple = Color(0xFF1D1B31)      // Deep Dark Purple
val UnabPurpleLight = Color(0xFF5B2C8C) // Standard UNAB Purple
val UnabOrange = Color(0xFFE9A427)      // UNAB Orange

// Light theme — warm white background, orange primary, purple disappears
val LightColorScheme = lightColorScheme(
    primary = UnabOrange,
    onPrimary = Color(0xFF1A1200),
    primaryContainer = Color(0xFFFFF0C6),
    onPrimaryContainer = Color(0xFF261900),
    secondary = Color(0xFF7A5200),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDA8),
    onSecondaryContainer = Color(0xFF261900),
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1D1B1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B1E),
    surfaceVariant = Color(0xFFF0EBE0),
    onSurfaceVariant = Color(0xFF4E4539),
    outline = Color(0xFF80746B),
    outlineVariant = Color(0xFFD4C8BE),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000),
)

// Purple-primary ("dark") theme
val DarkColorScheme = darkColorScheme(
    primary = UnabPurpleLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1A68),
    onPrimaryContainer = Color.White,
    secondary = UnabOrange,
    onSecondary = Color(0xFF1D1B31),
    secondaryContainer = Color(0xFF3D2A00),
    onSecondaryContainer = Color.White,
    background = UnabPurple,
    onBackground = Color.White,
    surface = UnabPurple,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2D1050),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFF4D1B1B),
    onErrorContainer = Color(0xFFFF8A80),
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
