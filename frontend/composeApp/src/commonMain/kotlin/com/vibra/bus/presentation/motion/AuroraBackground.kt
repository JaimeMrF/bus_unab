package com.vibra.bus.presentation.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.material3.MaterialTheme
import com.vibra.bus.presentation.theme.LocalIsDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

/**
 * Fondo tipo aurora/mesh: tres manchas de color difuminadas (radial gradients) que derivan
 * lentamente. Un único infiniteTransition; el progreso se lee en la fase de dibujo.
 *
 * - Movimiento normal: animado mientras la app está en primer plano.
 * - "Reducir movimiento": mismas manchas, quietas.
 * - Gama baja: un degradado vertical plano (sin aurora).
 */
@Composable
fun Modifier.auroraBackground(
    primary: Color = MaterialTheme.colorScheme.primary,
    secondary: Color = MaterialTheme.colorScheme.tertiary,
    tertiary: Color = MaterialTheme.colorScheme.secondary,
    base: Color = MaterialTheme.colorScheme.background,
): Modifier {
    val env = LocalMotion.current
    val dark = LocalIsDarkTheme.current
    val strength = if (dark) 0.34f else 0.20f

    val progress: State<Float>? = if (env.ambient) {
        rememberInfiniteTransition(label = "aurora").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(MotionSpec.AuroraLoopMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "aurora_t",
        )
    } else null

    val flat = env.lowTier
    val mid = env.midTier
    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val radius = maxOf(w, h) * 0.62f
        // Los brushes se crean una vez por tamaño/colores y se trasladan al dibujar.
        val b1 = Brush.radialGradient(listOf(primary.copy(alpha = strength), Color.Transparent), Offset.Zero, radius)
        val b2 = Brush.radialGradient(listOf(secondary.copy(alpha = strength * 0.85f), Color.Transparent), Offset.Zero, radius * 0.9f)
        val b3 = Brush.radialGradient(listOf(tertiary.copy(alpha = strength * 0.7f), Color.Transparent), Offset.Zero, radius * 0.8f)
        val flatBrush = Brush.verticalGradient(listOf(primary.copy(alpha = strength * 0.55f), Color.Transparent), 0f, h * 0.6f)
        onDrawBehind {
            drawRect(base)
            if (flat) {
                drawRect(flatBrush)
            } else {
                val t = (progress?.value ?: 0.12f) * TWO_PI
                translate(w * (0.25f + 0.12f * sin(t)), h * (0.18f + 0.08f * cos(t * 1f))) {
                    drawCircle(b1, radius, Offset.Zero)
                }
                translate(w * (0.80f + 0.10f * cos(t + 1.7f)), h * (0.35f + 0.10f * sin(t * 2f + 0.6f))) {
                    drawCircle(b2, radius * 0.9f, Offset.Zero)
                }
                if (!mid) translate(w * (0.45f + 0.15f * sin(t * 2f + 2.4f)), h * (0.92f + 0.06f * cos(t + 0.9f))) {
                    drawCircle(b3, radius * 0.8f, Offset.Zero)
                }
            }
        }
    }
}

/** Contenedor con aurora de fondo. */
@Composable
fun AuroraBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.auroraBackground()) { content() }
}
