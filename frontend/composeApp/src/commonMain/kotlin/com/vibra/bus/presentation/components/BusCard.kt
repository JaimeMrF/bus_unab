package com.vibra.bus.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.OccupancyDto
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.theme.VibraBusThemeUtils
import com.vibra.bus.presentation.theme.vibraBusColors

@Composable
fun BusCard(
    bus: BusSummaryDto,
    occupancy: OccupancyDto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false
) {
    val isFull = occupancy?.level == "full"
    val isAvailable = occupancy != null
    val interactionSource = remember { MutableInteractionSource() }
    
    // Animation states
    val pressedScale by animateFloatAsState(
        targetValue = if (interactionSource.collectIsPressedAsState().value) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh,
        ),
        label = "bus_card_scale",
    )

    // Use Material 3 theme utilities for colors
    val statusColor = VibraBusThemeUtils.busStatusColor(isAvailable, isFull)
    val occupancyColor = occupancy?.level?.let { VibraBusThemeUtils.occupancyColor(it) }
        ?: MaterialTheme.colorScheme.outline
    
    val statusLabel = when {
        isFull -> "Lleno"
        isAvailable -> "Disponible"
        else -> "Próximo"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .scale(pressedScale)
            .animateContentSize(
                animationSpec = tween(durationMillis = 300)
            )
            .shadow(
                elevation = 6.dp,
                shape = VibraBusShapes.Card,
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(VibraBusShapes.Card)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.vibraBusColors.glassSurface,
                        MaterialTheme.vibraBusColors.glassBorder
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.vibraBusColors.glassHighlight,
                        MaterialTheme.vibraBusColors.glassBorder
                    )
                ),
                shape = VibraBusShapes.Card
            )
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(
                    color = MaterialTheme.colorScheme.primary,
                    radius = 24.dp
                )
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Bus icon box premium con Material 3
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(VibraBusShapes.BusMarker)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🚌",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = bus.name,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.2.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = bus.plate,
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(5.dp))
                // Status row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text       = statusLabel,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = statusColor,
                    )
                }
            }

            // Occupancy badge
            occupancy?.let {
                OccupancyBadge(
                    level      = it.level,
                    percentage = it.percentage,
                    color      = occupancyColor,
                )
                Spacer(Modifier.width(8.dp))
            }

            // Chevron
            Text(
                text  = "›",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun OccupancyBadge(level: String, percentage: Float, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "${percentage.toInt()}%",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = color,
            )
            Text(
                text     = level.replaceFirstChar { it.uppercase() },
                fontSize = 10.sp,
                color    = color,
            )
        }
    }
}