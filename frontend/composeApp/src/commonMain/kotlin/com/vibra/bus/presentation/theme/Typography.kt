package com.vibra.bus.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.doctor_glitch
import vibrabus.composeapp.generated.resources.poppins_bold
import vibrabus.composeapp.generated.resources.poppins_extrabold
import vibrabus.composeapp.generated.resources.poppins_light
import vibrabus.composeapp.generated.resources.poppins_medium
import vibrabus.composeapp.generated.resources.poppins_regular
import vibrabus.composeapp.generated.resources.poppins_semibold

@Composable
fun poppinsFamily(): FontFamily = FontFamily(
    Font(Res.font.poppins_light,     FontWeight.Light),
    Font(Res.font.poppins_regular,   FontWeight.Normal),
    Font(Res.font.poppins_medium,    FontWeight.Medium),
    Font(Res.font.poppins_semibold,  FontWeight.SemiBold),
    Font(Res.font.poppins_bold,      FontWeight.Bold),
    Font(Res.font.poppins_extrabold, FontWeight.ExtraBold),
)

@Composable
fun glitchFamily(): FontFamily = FontFamily(
    Font(Res.font.doctor_glitch, FontWeight.Normal)
)

// Alias for compatibility if needed, but we'll update the screens
@Composable
fun rubikGlitchFamily(): FontFamily = glitchFamily()

@Composable
fun appTypography(): Typography {
    val poppins = poppinsFamily()
    return Typography(
        displayLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Bold,
            fontSize   = 32.sp,
            lineHeight = 40.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Bold,
            fontSize   = 24.sp,
            lineHeight = 32.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 20.sp,
            lineHeight = 28.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 18.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Medium,
            fontSize   = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Normal,
            fontSize   = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp,
            lineHeight = 20.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Normal,
            fontSize   = 12.sp,
            lineHeight = 16.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Medium,
            fontSize   = 12.sp,
            lineHeight = 16.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = poppins,
            fontWeight = FontWeight.Medium,
            fontSize   = 11.sp,
            lineHeight = 16.sp,
        ),
    )
}

val AppTypography = Typography()
