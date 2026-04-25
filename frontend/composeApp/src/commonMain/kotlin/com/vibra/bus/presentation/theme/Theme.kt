package com.vibra.bus.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = AppColors.PrimaryPurple,
    onPrimary = AppColors.White,
    primaryContainer = AppColors.PrimaryBg,
    secondary = AppColors.AccentOrange,
    onSecondary = AppColors.White,
    background = AppColors.PrimaryBg,
    onBackground = AppColors.White,
    surface = AppColors.DarkHeader,
    onSurface = AppColors.White,
    error = AppColors.Red,
)

@Composable
fun VibraBusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
