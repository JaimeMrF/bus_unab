package com.vibra.bus.presentation.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.vibra.bus.presentation.theme.Motion

/** Plataforma: "reducir movimiento" del sistema (Android ANIMATOR_DURATION_SCALE == 0, iOS Reduce Motion). */
@Composable
expect fun platformReduceMotion(): Boolean

/** Plataforma: dispositivo de gama baja (RAM baja / API < 26 / poca memoria física). */
@Composable
expect fun platformLowTier(): Boolean

/** Plataforma: la app está en primer plano (las animaciones infinitas se pausan si no). */
@Composable
expect fun platformAppActive(): Boolean

/**
 * Entorno de movimiento de la app.
 * - [reduceMotion]: versión estática de todo (sin loops ni desplazamientos).
 * - [lowTier]: además sin efectos ambientales caros (aurora, confeti, ondas).
 * - [appActive]: en segundo plano no se mantiene ningún loop.
 */
@Immutable
class MotionEnv(
    val reduceMotion: Boolean,
    val lowTier: Boolean,
    val appActive: Boolean,
) {
    /** Transiciones cortas de una sola vez (entradas, press, contadores). */
    val animate: Boolean get() = !reduceMotion

    /** Efectos ambientales continuos o costosos: aurora, ondas, confeti. */
    val ambient: Boolean get() = !reduceMotion && !lowTier && appActive
}

val LocalMotion = compositionLocalOf { MotionEnv(reduceMotion = false, lowTier = false, appActive = true) }

@Composable
fun rememberReduceMotion(): Boolean = platformReduceMotion()

/** Provee [LocalMotion] a toda la app; colocar una vez en la raíz. */
@Composable
fun ProvideMotion(content: @Composable () -> Unit) {
    val reduce = platformReduceMotion()
    val low = platformLowTier()
    val active = platformAppActive()
    val env = remember(reduce, low, active) { MotionEnv(reduce, low, active) }
    CompositionLocalProvider(LocalMotion provides env, content = content)
}

/** Duraciones y resortes compartidos por todas las animaciones. */
object MotionSpec {
    const val Fast = Motion.fast
    const val Standard = Motion.standard
    const val Emphasized = Motion.emphasized
    const val Counter = 900
    const val ConfettiMillis = 1500
    const val AuroraLoopMillis = 28_000
    const val PulseMillis = 1800
    const val StaggerStepMillis = 55L
    const val StaggerMaxIndex = 8

    fun <T> enter(env: MotionEnv) = if (env.animate) tween<T>(Emphasized, easing = Motion.easeOut) else tween<T>(0)

    /** Resorte con rebote leve para indicadores y pop de iconos. */
    fun <T> springy(env: MotionEnv) =
        if (env.animate) spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium) else tween<T>(0)
}
