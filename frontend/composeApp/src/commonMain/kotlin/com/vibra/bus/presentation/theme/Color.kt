package com.vibra.bus.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── BUCARATRANSIT · Bucaramanga — identidad ─────────────────────────────────
// Colores tomados del logo oficial: azul rey (marca) + amarillo (acento del bus).
val ReyBlue       = Color(0xFF01265A)  // azul rey — color de marca
val ReyBlueDeep   = Color(0xFF001A3D)  // azul rey profundo (fondo oscuro)
val TransitYellow = Color(0xFFFCBB01)  // amarillo BUCARATRANSIT (acento)

// Light theme — fondo claro, azul rey primario, amarillo acento
val LightColorScheme = lightColorScheme(
    primary = ReyBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F5),
    onPrimaryContainer = Color(0xFF001A3D),
    secondary = TransitYellow,
    onSecondary = Color(0xFF241A00),
    secondaryContainer = Color(0xFFFFE7A3),
    onSecondaryContainer = Color(0xFF241A00),
    tertiary = Color(0xFF2A6FD6),
    onTertiary = Color.White,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF0E1622),
    surface = Color.White,
    onSurface = Color(0xFF0E1622),
    surfaceVariant = Color(0xFFE4E9F2),
    onSurfaceVariant = Color(0xFF454F5E),
    outline = Color(0xFF737C8C),
    outlineVariant = Color(0xFFC6CEDB),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000),
)

// Dark theme — azul rey profundo, amarillo primario (acento visible), azul claro secundario
val DarkColorScheme = darkColorScheme(
    primary = TransitYellow,
    onPrimary = Color(0xFF241A00),
    primaryContainer = Color(0xFF3A2C00),
    onPrimaryContainer = Color(0xFFFFE7A3),
    secondary = Color(0xFFA9C6F0),
    onSecondary = Color(0xFF0A2A5C),
    secondaryContainer = Color(0xFF1E3763),
    onSecondaryContainer = Color(0xFFDCE6F5),
    tertiary = Color(0xFFFFD27A),
    onTertiary = Color(0xFF241A00),
    background = ReyBlueDeep,
    onBackground = Color(0xFFE6EBF3),
    surface = Color(0xFF0A2148),
    onSurface = Color(0xFFE6EBF3),
    surfaceVariant = Color(0xFF1E3763),
    onSurfaceVariant = Color(0xFFC2CDE0),
    outline = Color(0xFF8A96A8),
    outlineVariant = Color(0xFF2C3E5C),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFF4D1B1B),
    onErrorContainer = Color(0xFFFF8A80),
    scrim = Color(0xFF000000),
)

object GlassColors {
    val Surface = Color(0x1AFFFFFF)
    val Border = Color(0x33FFFFFF)
    val Highlight = Color(0x4DFFFFFF)
    val SurfaceDark = Color(0xB3001A3D)   // 70% azul rey glass
    val BorderDark = Color(0x33FFFFFF)    // 20% White Border
    val HighlightDark = Color(0x1AFFFFFF) // 10% White Highlight
}

object TransportColors {
    val Success = Color(0xFF2EBE6C)
    val Warning = Color(0xFFFFB020)
    val BusAvailable = Color(0xFF2EBE6C)
    val BusFull = Color(0xFFE5484D)
    val BusApproaching = TransitYellow
    val RouteActive = TransitYellow
    val RouteInactive = Color(0xFF94A3B8)
    val OccupancyLow = Color(0xFF2EBE6C)
    val OccupancyMedium = TransitYellow
    val OccupancyHigh = Color(0xFFF59E0B)
}
