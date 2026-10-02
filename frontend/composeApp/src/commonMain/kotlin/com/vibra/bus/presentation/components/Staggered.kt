package com.vibra.bus.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vibra.bus.presentation.motion.staggerIn

/** Entrada escalonada (fade + subida corta) por graphicsLayer; respeta "reducir movimiento". */
@Composable
fun Staggered(
    index: Int,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.staggerIn(index)) { content() }
}
