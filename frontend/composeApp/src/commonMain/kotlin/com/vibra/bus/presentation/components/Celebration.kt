package com.vibra.bus.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibra.bus.domain.brand.MascotPose
import com.vibra.bus.presentation.motion.ConfettiBurst
import com.vibra.bus.presentation.motion.LocalMotion
import com.vibra.bus.presentation.theme.LocalBrand
import kotlinx.coroutines.delay

/**
 * Celebracion de un hito (pago, recarga, llegada): la mascota en la pose [pose] aparece con un
 * pequeno rebote durante ~1.8 s y, con [confetti], lanza el confeti ligero. No intercepta toques.
 * Cada vez que [trigger] sube (y es mayor que 0) se reproduce de nuevo. Sin mascota solo hay confeti.
 */
@Composable
fun CelebrationOverlay(
    trigger: Int,
    modifier: Modifier = Modifier,
    pose: MascotPose = MascotPose.Celebrating,
    confetti: Boolean = pose == MascotPose.Celebrating,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
            delay(1_800)
            visible = false
        }
    }
    val hasPose = LocalBrand.current.poseUrl(pose) != null
    val env = LocalMotion.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (confetti) ConfettiBurst(trigger = trigger, modifier = Modifier.fillMaxSize())
        if (hasPose) {
            AnimatedVisibility(
                visible = visible,
                enter = if (env.animate) scaleIn(spring(0.55f, Spring.StiffnessMedium), initialScale = 0.4f) + fadeIn() else fadeIn(),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
            ) {
                BrandMascot(pose = pose, size = 160.dp, animated = false)
            }
        }
    }
}
