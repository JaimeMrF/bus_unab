package com.vibra.bus.presentation.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.bus.presentation.theme.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Confeti ──────────────────────────────────────────────────────────────────

private const val CONFETTI_COUNT = 36 // tope duro: ≤ 40 partículas

private class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val colorIndex: Int,
)

/**
 * Ráfaga de confeti ligera: [CONFETTI_COUNT] rectángulos en un Canvas durante 1.5 s.
 * Cada vez que [trigger] cambia (y es > 0) se lanza una ráfaga. No hace nada con
 * "reducir movimiento" ni en gama baja. [origin] es fracción (x, y) del área.
 */
@Composable
fun ConfettiBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer,
    ),
    origin: Offset = Offset(0.5f, 0.35f),
) {
    val env = LocalMotion.current
    if (!env.rich || trigger <= 0) return

    val particles = remember(trigger) {
        val rnd = Random(trigger * 7919)
        List(CONFETTI_COUNT) {
            Particle(
                angle = (-PI.toFloat()) * (0.1f + 0.8f * rnd.nextFloat()), // abanico hacia arriba
                speed = 0.35f + 0.65f * rnd.nextFloat(),
                size = 5f + 6f * rnd.nextFloat(),
                spin = 360f * (rnd.nextFloat() - 0.5f) * 2f,
                colorIndex = rnd.nextInt(Int.MAX_VALUE),
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(MotionSpec.ConfettiMillis, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val reach = size.minDimension * 0.9f
        val ox = size.width * origin.x
        val oy = size.height * origin.y
        val ease = 1f - (1f - p) * (1f - p) // desacelera al subir
        val fade = (1f - ((p - 0.6f) / 0.4f).coerceIn(0f, 1f))
        for (part in particles) {
            val x = ox + cos(part.angle) * part.speed * reach * ease
            val y = oy + sin(part.angle) * part.speed * reach * ease + 0.9f * size.height * p * p
            rotate(degrees = part.spin * p, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[part.colorIndex % colors.size].copy(alpha = fade),
                    topLeft = Offset(x - part.size / 2f, y - part.size / 2f),
                    size = Size(part.size, part.size * 0.55f),
                )
            }
        }
    }
}

// ── Contador animado ─────────────────────────────────────────────────────────

/**
 * Texto numérico que "cuenta" hacia [target]. El valor intermedio se dibuja con
 * `drawText` en la fase de dibujo (sin recomponer cada frame); el tamaño se reserva
 * midiendo el valor final. Lector de pantalla: lee siempre el valor final.
 */
@Composable
fun CountUpText(
    target: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    format: (Long) -> String = { it.toString() },
) {
    val env = LocalMotion.current
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val resolved = style.copy(color = color)
    val finalText = format(target)
    val finalLayout = remember(finalText, resolved) { measurer.measure(finalText, resolved) }

    val value = remember { Animatable(if (env.animate) 0f else target.toFloat()) }
    LaunchedEffect(target, env.animate) {
        if (env.animate) value.animateTo(target.toFloat(), tween(MotionSpec.Counter, easing = FastOutSlowInEasing))
        else value.snapTo(target.toFloat())
    }

    val w: Dp = with(density) { finalLayout.size.width.toDp() }
    val h: Dp = with(density) { finalLayout.size.height.toDp() }
    Canvas(
        modifier = modifier
            .size(w, h)
            .semantics { contentDescription = finalText },
    ) {
        val current = format(value.value.toLong())
        drawText(measurer, current, Offset.Zero, resolved)
    }
}

// ── Anillo de cuenta regresiva ───────────────────────────────────────────────

/**
 * Anillo que se vacía de forma continua (interpola entre los ticks de 1 s). El barrido se
 * lee en la fase de dibujo. Color de aviso cuando quedan ≤ [warnAt] segundos.
 */
@Composable
fun CountdownRing(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 5.dp,
    warnAt: Int = 10,
    content: @Composable () -> Unit = {},
) {
    val env = LocalMotion.current
    val sweep = animateFloatAsState(
        targetValue = (remaining.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = if (env.animate) tween(1000, easing = LinearEasing) else tween(0),
        label = "countdown_ring",
    )
    val warn = remaining <= warnAt
    val ringColor = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Box(modifier = modifier.size(size), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            drawArc(ringColor, -90f, 360f * sweep.value, false, Offset(inset, inset), arcSize, style = stroke)
        }
        content()
    }
}

// ── Ondas de pulso ───────────────────────────────────────────────────────────

/**
 * Ondas concéntricas que se expanden y se desvanecen (estado "llegando"/espera). Un solo
 * infiniteTransition leído al dibujar. Sin movimiento ambiental dibuja un anillo estático.
 */
@Composable
fun PulseRings(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    rings: Int = 3,
) {
    val env = LocalMotion.current
    val t = if (env.ambient) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(MotionSpec.PulseMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "pulse_t",
        )
    } else null

    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val maxR = size.minDimension / 2f
        val minR = maxR * 0.35f
        if (t == null) {
            drawCircle(color.copy(alpha = 0.25f), maxR * 0.8f, style = Stroke(width = 2.dp.toPx()))
            return@Canvas
        }
        for (i in 0 until rings) {
            val phase = (t.value + i.toFloat() / rings) % 1f
            val r = minR + (maxR - minR) * phase
            drawCircle(color.copy(alpha = 0.55f * (1f - phase)), r, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
