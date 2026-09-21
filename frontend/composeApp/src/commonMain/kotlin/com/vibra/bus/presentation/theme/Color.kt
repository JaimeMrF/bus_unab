package com.vibra.bus.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Leopardo · Bucaramanga — identidad ───────────────────────────────────────
// Bucaramanga = "Ciudad de los Parques" y el leopardo/tigrillo santandereano.
// Dorado del leopardo (primario), negro-café de las rosetas (fondo oscuro),
// verde de los parques (secundario/acento).
val LeopardGold     = Color(0xFFE8A33D)  // dorado leopardo
val LeopardEspresso = Color(0xFF17130E)  // negro-café cálido (rosetas)
val LeopardSand     = Color(0xFFFDF8F0)  // arena cálida (fondo claro)
val ParqueGreen     = Color(0xFF3D6B4F)  // verde parques de Bucaramanga

// Light theme — warm ivory, leopard-gold primary, forest-green secondary
val LightColorScheme = lightColorScheme(
    primary = LeopardGold,
    onPrimary = Color(0xFF241807),
    primaryContainer = Color(0xFFFFE2B8),
    onPrimaryContainer = Color(0xFF211604),
    secondary = ParqueGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5EED2),
    onSecondaryContainer = Color(0xFF00200F),
    tertiary = Color(0xFF9C5F00),
    onTertiary = Color.White,
    background = LeopardSand,
    onBackground = Color(0xFF211C14),
    surface = Color.White,
    onSurface = Color(0xFF211C14),
    surfaceVariant = Color(0xFFF0E9DA),
    onSurfaceVariant = Color(0xFF4F4739),
    outline = Color(0xFF817768),
    outlineVariant = Color(0xFFD3C7B5),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000),
)

// Dark theme — espresso/rosetas, gold primary, lighter green secondary
val DarkColorScheme = darkColorScheme(
    primary = LeopardGold,
    onPrimary = Color(0xFF3E2A00),
    primaryContainer = Color(0xFF5C4100),
    onPrimaryContainer = Color(0xFFFFE2B8),
    secondary = Color(0xFF8FD49F),
    onSecondary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF245233),
    onSecondaryContainer = Color(0xFFC5EED2),
    tertiary = Color(0xFFFFC76E),
    onTertiary = Color(0xFF452A00),
    background = LeopardEspresso,
    onBackground = Color(0xFFECE3D4),
    surface = Color(0xFF1E1912),
    onSurface = Color(0xFFECE3D4),
    surfaceVariant = Color(0xFF4C4537),
    onSurfaceVariant = Color(0xFFCFC5B3),
    outline = Color(0xFF988F7E),
    outlineVariant = Color(0xFF3B372C),
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
    val SurfaceDark = Color(0xB317130E)   // 70% espresso glass
    val BorderDark = Color(0x33FFFFFF)    // 20% White Border
    val HighlightDark = Color(0x1AFFFFFF) // 10% White Highlight
}

object TransportColors {
    val Success = Color(0xFF2EBE6C)
    val Warning = Color(0xFFFFB020)
    val BusAvailable = Color(0xFF2EBE6C)
    val BusFull = Color(0xFFE5484D)
    val BusApproaching = Color(0xFFE8A33D)
    val RouteActive = Color(0xFFE8A33D)
    val RouteInactive = Color(0xFF94A3B8)
    val OccupancyLow = Color(0xFF2EBE6C)
    val OccupancyMedium = Color(0xFFE8A33D)
    val OccupancyHigh = Color(0xFFF59E0B)
}
