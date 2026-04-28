package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.theme.VibraBusThemeUtils
import com.vibra.bus.util.LatLng

@Composable
expect fun MapViewComposable(
    modifier: Modifier,
    userLocation: LatLng?,
    showStops: Boolean,
    selectedStop: StopDto?,
    onStopSelected: (StopDto) -> Unit,
    stops: List<StopDto>,
    buses: List<BusSummaryDto>
)

/**
 * Custom bus marker for map with Material 3 styling
 */
@Composable
fun BusMarker(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "bus_marker_scale"
    )
    
    val busColor = VibraBusThemeUtils.busStatusColor(true, false)
    
    Box(
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .clip(VibraBusShapes.BusMarker)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        busColor,
                        busColor.copy(alpha = 0.8f)
                    ),
                    center = Offset(20f, 20f),
                    radius = 20f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Bus icon placeholder
        androidx.compose.material3.Text(
            text = "🚌",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * Custom stop marker for map with Material 3 styling
 */
@Composable
fun StopMarker(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.3f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "stop_marker_scale"
    )
    
    val stopColor = VibraBusThemeUtils.routeColor(isSelected)
    
    Box(
        modifier = modifier
            .size(32.dp)
            .scale(scale)
            .clip(VibraBusShapes.StopMarker)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        stopColor
                    ),
                    center = Offset(16f, 16f),
                    radius = 16f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Stop indicator
        Canvas(
            modifier = Modifier.size(12.dp)
        ) {
            drawCircle(
                color = stopColor,
                radius = size.minDimension / 2,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = stopColor,
                radius = size.minDimension / 4
            )
        }
    }
}

/**
 * User location marker with Material 3 styling
 */
@Composable
fun UserLocationMarker(
    modifier: Modifier = Modifier
) {
    val pulseScale by animateFloatAsState(
        targetValue = 1.5f,
        animationSpec = tween(durationMillis = 1000),
        label = "user_location_pulse"
    )
    
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(pulseScale)
                .clip(VibraBusShapes.MapButton)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
        )
        
        // Inner location dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(VibraBusShapes.MapButton)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * Map overlay controls with Material 3 styling
 */
@Composable
fun MapOverlayControls(
    modifier: Modifier = Modifier,
    onCenterLocation: () -> Unit = {},
    onToggleStops: () -> Unit = {},
    showStops: Boolean = false
) {
    AnimatedVisibility(
        visible = true,
        enter = scaleIn(animationSpec = tween(durationMillis = 300)) + fadeIn(),
        exit = scaleOut(animationSpec = tween(durationMillis = 300)) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            // Map control buttons
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                // Center location button
                FloatingActionButton(
                    onClick = onCenterLocation,
                    modifier = Modifier.size(48.dp),
                    shape = VibraBusShapes.MapButton,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Center location",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Toggle stops button
                FloatingActionButton(
                    onClick = onToggleStops,
                    modifier = Modifier.size(48.dp),
                    shape = VibraBusShapes.MapButton,
                    containerColor = if (showStops) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surface,
                    contentColor = if (showStops) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        if (showStops) Icons.Default.Visibility 
                        else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle stops",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
