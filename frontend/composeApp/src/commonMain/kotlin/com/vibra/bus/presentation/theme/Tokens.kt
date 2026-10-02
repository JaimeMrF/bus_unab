package com.vibra.bus.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

/** Escala de espaciado (base 4dp). Usar tokens, no números sueltos. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val huge = 48.dp
}

/** Elevaciones tonales sutiles. */
object Elevation {
    val none = 0.dp
    val low = 1.dp
    val medium = 3.dp
    val high = 6.dp
}

/** Tamaños mínimos de accesibilidad. */
object Sizing {
    /** Área táctil mínima (Material/WCAG: 48dp). */
    val touchTarget = 48.dp
    val iconSm = 18.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
}

/** Duraciones y curvas de movimiento. */
object Motion {
    const val fast = 120
    const val standard = 220
    const val emphasized = 360
    val easeOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easeInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}
