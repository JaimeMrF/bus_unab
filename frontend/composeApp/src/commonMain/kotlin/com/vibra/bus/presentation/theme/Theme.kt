package com.vibra.bus.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary          = AppColors.PrimaryPurple,
    onPrimary        = AppColors.White,
    primaryContainer = Color(0xFFF0E0FF),
    secondary        = AppColors.AccentOrange,
    onSecondary      = AppColors.White,
    background       = AppColors.PrimaryBg,
    onBackground     = AppColors.TextPrimary,
    surface          = AppColors.White,
    onSurface        = AppColors.TextPrimary,
    error            = AppColors.Red,
    onError          = AppColors.White,
)

@Composable
fun VibraBusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = appTypography(),   // ← Poppins cargado desde recursos
        shapes      = AppShapes,
        content     = content,
    )
}
