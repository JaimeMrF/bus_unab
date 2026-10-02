package com.vibra.bus.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vibra.bus.presentation.theme.Motion

/** Entrada escalonada (fade + desplazamiento corto) para componer pantallas con ritmo. */
@Composable
fun Staggered(
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.emphasized, delayMillis = index * 70, easing = Motion.easeOut)) +
            slideInVertically(tween(Motion.emphasized, delayMillis = index * 70, easing = Motion.easeOut)) { it / 6 },
    ) {
        content()
    }
}
