package com.vibra.bus.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.bus.presentation.theme.AppShape

/** Placeholder de carga con brillo que usa tonos de la superficie del tema (sin colores de marca). */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer_progress",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val shine = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(AppShape.Card)
            // La animación se lee en la fase de dibujo: no recompone el árbol.
            .drawWithCache {
                val w = size.width
                onDrawBehind {
                    val x = -w + progress * 3f * w
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(base, shine, base),
                            start = Offset(x, 0f),
                            end = Offset(x + w, 0f),
                        )
                    )
                }
            }
            .clearAndSetSemantics { },
    )
}

@Composable
fun ShimmerBusCard() {
    ShimmerBox(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), height = 100.dp)
}

@Composable
fun ShimmerList(count: Int = 4, itemHeight: Dp = 88.dp) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) { ShimmerBox(height = itemHeight) }
    }
}
