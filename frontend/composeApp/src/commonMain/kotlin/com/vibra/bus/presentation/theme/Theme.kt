package com.vibra.bus.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// CompositionLocal for theme-specific values
data class VibraBusColors(
    val glassSurface: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val busAvailable: Color,
    val busFull: Color,
    val busApproaching: Color,
    val routeActive: Color,
    val routeInactive: Color,
    val occupancyLow: Color,
    val occupancyMedium: Color,
    val occupancyHigh: Color,
)

val LocalVibraBusColors = staticCompositionLocalOf<VibraBusColors> {
    error("No VibraBusColors provided")
}

// Light Theme Colors
private val LightVibraBusColors = VibraBusColors(
    glassSurface = GlassColors.Surface,
    glassBorder = GlassColors.Border,
    glassHighlight = GlassColors.Highlight,
    busAvailable = TransportColors.BusAvailable,
    busFull = TransportColors.BusFull,
    busApproaching = TransportColors.BusApproaching,
    routeActive = TransportColors.RouteActive,
    routeInactive = TransportColors.RouteInactive,
    occupancyLow = TransportColors.OccupancyLow,
    occupancyMedium = TransportColors.OccupancyMedium,
    occupancyHigh = TransportColors.OccupancyHigh,
)

// Dark Theme Colors
private val DarkVibraBusColors = VibraBusColors(
    glassSurface = GlassColors.SurfaceDark,
    glassBorder = GlassColors.BorderDark,
    glassHighlight = GlassColors.HighlightDark,
    busAvailable = TransportColors.BusAvailable,
    busFull = TransportColors.BusFull,
    busApproaching = TransportColors.BusApproaching,
    routeActive = TransportColors.RouteActive,
    routeInactive = TransportColors.RouteInactive,
    occupancyLow = TransportColors.OccupancyLow,
    occupancyMedium = TransportColors.OccupancyMedium,
    occupancyHigh = TransportColors.OccupancyHigh,
)

/**
 * Main theme for VibraBus app with Material 3 design system
 * 
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use dynamic colors (Android 12+)
 * @param content The composable content to be themed
 */
@Composable
fun VibraBusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for brand consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val vibraBusColors = when {
        darkTheme -> DarkVibraBusColors
        else -> LightVibraBusColors
    }

    CompositionLocalProvider(LocalVibraBusColors provides vibraBusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}

/**
 * Extension property to access custom colors from MaterialTheme
 */
val MaterialTheme.vibraBusColors: VibraBusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalVibraBusColors.current

/**
 * Preview themes for composables
 */
@Composable
fun VibraBusLightTheme(
    content: @Composable () -> Unit
) {
    VibraBusTheme(
        darkTheme = false,
        dynamicColor = false,
        content = content
    )
}

@Composable
fun VibraBusDarkTheme(
    content: @Composable () -> Unit
) {
    VibraBusTheme(
        darkTheme = true,
        dynamicColor = false,
        content = content
    )
}

/**
 * Theme utilities for consistent color access
 */
object VibraBusThemeUtils {
    
    /**
     * Get appropriate glass colors based on theme
     */
    @Composable
    fun glassColors() = MaterialTheme.vibraBusColors.let {
        Triple(it.glassSurface, it.glassBorder, it.glassHighlight)
    }
    
    /**
     * Get bus status color
     */
    @Composable
    fun busStatusColor(isAvailable: Boolean, isFull: Boolean): Color {
        return when {
            isFull -> MaterialTheme.vibraBusColors.busFull
            isAvailable -> MaterialTheme.vibraBusColors.busAvailable
            else -> MaterialTheme.vibraBusColors.busApproaching
        }
    }
    
    /**
     * Get occupancy color
     */
    @Composable
    fun occupancyColor(level: String): Color {
        return when (level.lowercase()) {
            "low" -> MaterialTheme.vibraBusColors.occupancyLow
            "medium" -> MaterialTheme.vibraBusColors.occupancyMedium
            "high" -> MaterialTheme.vibraBusColors.occupancyHigh
            "full" -> MaterialTheme.vibraBusColors.busFull
            else -> MaterialTheme.colorScheme.outline
        }
    }
    
    /**
     * Get route color
     */
    @Composable
    fun routeColor(isActive: Boolean): Color {
        return if (isActive) {
            MaterialTheme.vibraBusColors.routeActive
        } else {
            MaterialTheme.vibraBusColors.routeInactive
        }
    }
}
