package com.vibra.bus.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.vibra.bus.presentation.motion.LocalMotion

/** Corazon de favorito: rebote al activarse, haptic sutil y area tactil de 48dp. */
@Composable
fun FavoriteHeart(
    favorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val env = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val pop = remember { Animatable(1f) }
    LaunchedEffect(favorite) {
        if (favorite && env.animate) {
            pop.snapTo(0.6f)
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
    }
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onToggle()
        },
        modifier = modifier.semantics { stateDescription = if (favorite) "Favorita" else "No favorita" },
    ) {
        Icon(
            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (favorite) "Quitar de favoritas" else "Agregar a favoritas",
            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer {
                val v = pop.value
                scaleX = v
                scaleY = v
            },
        )
    }
}
