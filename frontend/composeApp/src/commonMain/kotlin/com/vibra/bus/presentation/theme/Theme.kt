package com.vibra.bus.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vibra.bus.domain.brand.BrandConfig

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}

/** Marca activa: nombre, logo, mascota, features. Siempre disponible dentro de [AppTheme]. */
val LocalBrand = staticCompositionLocalOf { BrandConfig.Neutral }

val LocalIsDarkTheme = compositionLocalOf { true }
val LocalThemeToggle = compositionLocalOf<(Boolean) -> Unit> { {} }

/**
 * Tema único de la app. Todo el aspecto (color, tipografía, radios) se deriva de [brand];
 * cambiar el BrandConfig cambia toda la interfaz.
 */
@Composable
fun AppTheme(
    brand: BrandConfig = BrandConfig.Neutral,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) brand.colors.dark else brand.colors.light
    val colorScheme = remember(palette, darkTheme) { palette.toColorScheme(darkTheme) }
    val appColors = remember(palette, darkTheme) { palette.toAppColors(darkTheme, colorScheme) }
    val scale = radiusScale(brand.cornerRadius)
    val shapeSet = remember(scale) { AppShapeSet(scale) }
    val shapes = remember(scale) { materialShapes(scale) }
    val family = brandFontFamily(brand.fontFamily)
    val typography = remember(family) { appTypography(family) }

    CompositionLocalProvider(
        LocalBrand provides brand,
        LocalAppColors provides appColors,
        LocalAppShapes provides shapeSet,
        LocalIsDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

val MaterialTheme.brand: BrandConfig
    @Composable
    @ReadOnlyComposable
    get() = LocalBrand.current

object AppThemeUtils {

    @Composable
    fun glassColors() = MaterialTheme.appColors.let {
        Triple(it.glassSurface, it.glassBorder, it.glassHighlight)
    }

    @Composable
    fun busStatusColor(isAvailable: Boolean, isFull: Boolean): Color = when {
        isFull -> MaterialTheme.appColors.busFull
        isAvailable -> MaterialTheme.appColors.busAvailable
        else -> MaterialTheme.appColors.busApproaching
    }

    @Composable
    fun occupancyColor(level: String): Color = when (level.lowercase()) {
        "low" -> MaterialTheme.appColors.occupancyLow
        "medium" -> MaterialTheme.appColors.occupancyMedium
        "high" -> MaterialTheme.appColors.occupancyHigh
        "full" -> MaterialTheme.appColors.busFull
        else -> MaterialTheme.colorScheme.outline
    }

    @Composable
    fun routeColor(isActive: Boolean): Color =
        if (isActive) MaterialTheme.appColors.routeActive else MaterialTheme.appColors.routeInactive
}
