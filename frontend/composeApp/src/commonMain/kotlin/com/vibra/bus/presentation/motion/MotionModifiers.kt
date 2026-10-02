package com.vibra.bus.presentation.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vibra.bus.presentation.theme.Motion
import kotlinx.coroutines.delay

/**
 * Escala sutil al presionar. El valor animado se lee dentro de graphicsLayer (fase de dibujo):
 * no recompone nada. Con "reducir movimiento" no escala.
 */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.97f): Modifier {
    val env = LocalMotion.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(source, env.animate) {
        source.interactions.collect { interaction ->
            val target = when (interaction) {
                is PressInteraction.Press -> if (env.animate) pressedScale else 1f
                is PressInteraction.Release, is PressInteraction.Cancel -> 1f
                else -> return@collect
            }
            scale.animateTo(target, tween(Motion.fast, easing = Motion.easeOut))
        }
    }
    return this.graphicsLayer {
        val s = scale.value
        scaleX = s
        scaleY = s
    }
}

/**
 * Entrada escalonada (fade + subida corta) por graphicsLayer. Solo los primeros
 * [MotionSpec.StaggerMaxIndex] elementos se animan; el resto aparece al instante para no
 * "reproducir" la animación al hacer scroll.
 */
@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val env = LocalMotion.current
    val animated = env.animate && index <= MotionSpec.StaggerMaxIndex
    val progress = remember { Animatable(if (animated) 0f else 1f) }
    if (animated) {
        LaunchedEffect(Unit) {
            delay(index.coerceAtLeast(0) * MotionSpec.StaggerStepMillis)
            progress.animateTo(1f, tween(MotionSpec.Emphasized, easing = Motion.easeOut))
        }
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 20.dp.toPx()
    }
}

/**
 * Parallax/colapso de cabecera: se desplaza a [factor] de la velocidad del scroll y se
 * desvanece al llegar a [fadeDistance]. [offset] devuelve el scroll en px y se lee en
 * graphicsLayer, no en composición.
 */
@Composable
fun Modifier.parallaxCollapse(
    offset: () -> Float,
    factor: Float = 0.45f,
    fadeDistance: Float = 480f,
): Modifier {
    val env = LocalMotion.current
    if (!env.animate) return this
    return this.graphicsLayer {
        val o = offset().coerceAtLeast(0f)
        translationY = o * factor
        alpha = (1f - o / fadeDistance).coerceIn(0f, 1f)
        val s = (1f - o / (fadeDistance * 6f)).coerceIn(0.92f, 1f)
        scaleX = s
        scaleY = s
    }
}
