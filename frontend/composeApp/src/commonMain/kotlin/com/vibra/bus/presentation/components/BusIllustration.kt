package com.vibra.bus.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.bus.presentation.motion.LocalMotion
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Bus visto desde arriba con sus asientos: los ocupados se pintan con el color semantico de la
 * ocupacion y se llenan en cascada al aparecer o cambiar el valor. El progreso animado se lee
 * al dibujar (sin recomponer). [percentage] va de 0 a 100.
 */
@Composable
fun BusIllustration(
    percentage: Float,
    modifier: Modifier = Modifier,
    width: Dp = 44.dp,
    height: Dp = 76.dp,
    bodyColor: Color = MaterialTheme.colorScheme.primary,
    occupiedColor: Color = MaterialTheme.colorScheme.error,
    seats: Int = 20,
) {
    val env = LocalMotion.current
    val target = (percentage / 100f).coerceIn(0f, 1f)
    val progress = remember { Animatable(if (env.animate) 0f else target) }
    LaunchedEffect(target, env.animate) {
        if (env.animate) progress.animateTo(target, tween(700, easing = FastOutSlowInEasing))
        else progress.snapTo(target)
    }
    val shell = MaterialTheme.colorScheme.surfaceVariant
    val empty = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val glass = lerp(bodyColor, Color.Black, 0.55f)
    val rows = ceil(seats / 2f).toInt()
    val description = "Ocupación ${(target * 100).roundToInt()} por ciento"

    Canvas(modifier = modifier.size(width, height).semantics { contentDescription = description }) {
        val w = size.width
        val h = size.height
        val r = w * 0.28f
        // Carroceria y borde del color del bus
        drawRoundRect(shell, Offset.Zero, Size(w, h), CornerRadius(r))
        drawRoundRect(bodyColor, Offset.Zero, Size(w, h), CornerRadius(r), style = Stroke(width = w * 0.07f))
        // Parabrisas
        drawRoundRect(glass, Offset(w * 0.12f, h * 0.03f), Size(w * 0.76f, h * 0.09f), CornerRadius(w * 0.08f))
        // Asientos en 2 columnas con pasillo
        val top = h * 0.17f
        val area = h * 0.78f
        val rowH = area / rows
        val seatW = w * 0.3f
        val seatH = (rowH * 0.7f).coerceAtMost(seatW)
        val filled = progress.value * seats
        for (i in 0 until seats) {
            val row = i / 2
            val col = i % 2
            val x = if (col == 0) w * 0.14f else w - w * 0.14f - seatW
            val y = top + row * rowH + (rowH - seatH) / 2f
            val taken = i < filled
            val tone = if (taken) occupiedColor else empty
            drawRoundRect(tone, Offset(x, y), Size(seatW, seatH), CornerRadius(seatW * 0.28f))
            if (!taken) {
                drawRoundRect(outline, Offset(x, y), Size(seatW, seatH), CornerRadius(seatW * 0.28f), style = Stroke(width = 0.8f))
            }
        }
    }
}
