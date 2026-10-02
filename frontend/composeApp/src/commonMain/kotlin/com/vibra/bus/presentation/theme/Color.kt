package com.vibra.bus.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.vibra.bus.domain.brand.BrandPalette
import com.vibra.bus.domain.brand.parseHexColor

// ── Utilidades de contraste (WCAG) ──────────────────────────────────────────

fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val hi = maxOf(la, lb)
    val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

/** Negro o blanco, el que mejor se lea sobre [bg]. */
fun readableOn(bg: Color): Color =
    if (contrastRatio(Color.White, bg) >= contrastRatio(Color.Black, bg)) Color.White else Color.Black

/** Acerca [fg] a negro/blanco hasta alcanzar [min]:1 sobre [bg] (garantiza AA aunque la marca no lo cumpla). */
fun ensureContrast(fg: Color, bg: Color, min: Float = 4.5f): Color {
    if (contrastRatio(fg, bg) >= min) return fg
    val target = readableOn(bg)
    for (step in 1..20) {
        val c = lerp(fg, target, step / 20f)
        if (contrastRatio(c, bg) >= min) return c
    }
    return target
}

// ── Colores semánticos propios de la app (no existen en Material) ───────────

data class AppColors(
    val glassSurface: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val busAvailable: Color,
    val busFull: Color,
    val busApproaching: Color,
    val routeActive: Color,
    val routeInactive: Color,
    val occupancyLow: Color,
    val occupancyMedium: Color,
    val occupancyHigh: Color,
)

private fun BrandPalette.c(hex: String, fallback: Color) = parseHexColor(hex, fallback)

/** Genera el ColorScheme Material 3 completo a partir de las 11 claves de marca. */
fun BrandPalette.toColorScheme(dark: Boolean): ColorScheme {
    val fb = if (dark) Color(0xFF9DB6EA) else Color(0xFF2F4B7C)
    val primary = c(primary, fb)
    val onPrimary = ensureContrast(c(onPrimary, readableOn(primary)), primary)
    val secondary = c(secondary, fb)
    val onSecondary = ensureContrast(c(onSecondary, readableOn(secondary)), secondary)
    val background = c(background, if (dark) Color(0xFF0D1117) else Color(0xFFF6F7FA))
    val surface = c(surface, if (dark) Color(0xFF161B24) else Color.White)
    val onSurface = ensureContrast(c(onSurface, readableOn(surface)), surface, 7f)
    val accent = c(accent, primary)
    val error = c(error, Color(0xFFBA1A1A))

    val containerMix = if (dark) 0.78f else 0.86f
    val primaryContainer = lerp(primary, surface, containerMix)
    val secondaryContainer = lerp(secondary, surface, containerMix)
    val tertiaryContainer = lerp(accent, surface, containerMix)
    val errorContainer = lerp(error, surface, containerMix)
    val surfaceVariant = lerp(surface, onSurface, if (dark) 0.10f else 0.06f)

    fun tone(a: Float) = lerp(surface, onSurface, a)

    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = ensureContrast(primary, primaryContainer),
        inversePrimary = ensureContrast(primary, onSurface),
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = ensureContrast(secondary, secondaryContainer),
        tertiary = accent,
        onTertiary = readableOn(accent),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = ensureContrast(accent, tertiaryContainer),
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = ensureContrast(lerp(onSurface, surface, 0.28f), surfaceVariant),
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        outline = ensureContrast(lerp(onSurface, surface, 0.5f), surface, 3f),
        outlineVariant = tone(if (dark) 0.18f else 0.12f),
        error = error,
        onError = readableOn(error),
        errorContainer = errorContainer,
        onErrorContainer = ensureContrast(error, errorContainer),
        scrim = Color.Black,
        surfaceDim = if (dark) tone(0f) else lerp(surface, onSurface, 0.08f),
        surfaceBright = if (dark) tone(0.10f) else surface,
        surfaceContainerLowest = if (dark) lerp(background, Color.Black, 0.3f) else Color.White,
        surfaceContainerLow = tone(0.02f),
        surfaceContainer = tone(0.04f),
        surfaceContainerHigh = tone(0.07f),
        surfaceContainerHighest = tone(0.10f),
    )
}

fun BrandPalette.toAppColors(dark: Boolean, scheme: ColorScheme): AppColors {
    val success = c(success, Color(0xFF1E8E55))
    val warning = c(warning, Color(0xFFB26A00))
    val accent = c(accent, scheme.primary)
    val error = scheme.error
    return AppColors(
        glassSurface = if (dark) scheme.surface.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.78f),
        glassBorder = if (dark) Color.White.copy(alpha = 0.12f) else scheme.onSurface.copy(alpha = 0.08f),
        glassHighlight = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.9f),
        accent = accent,
        onAccent = readableOn(accent),
        success = success,
        warning = warning,
        busAvailable = success,
        busFull = error,
        busApproaching = warning,
        routeActive = scheme.primary,
        routeInactive = scheme.outline,
        occupancyLow = success,
        occupancyMedium = warning,
        occupancyHigh = lerp(warning, error, 0.5f),
    )
}
