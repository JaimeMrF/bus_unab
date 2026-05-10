package com.vibra.bus.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vibra.bus.data.model.StopDto
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.util.LatLng
import org.jetbrains.compose.resources.painterResource
import vibrabus.composeapp.generated.resources.Res
import vibrabus.composeapp.generated.resources.ic_bus_top

@Composable
expect fun MapViewComposable(
    modifier: Modifier,
    userLocation: LatLng?,
    showStops: Boolean,
    selectedStop: StopDto?,
    onStopSelected: (StopDto) -> Unit,
    selectedBus: com.vibra.bus.data.model.BusSummaryDto?,
    onBusSelected: (com.vibra.bus.data.model.BusSummaryDto) -> Unit,
    stops: List<com.vibra.bus.data.model.StopDto>,
    buses: List<com.vibra.bus.data.model.BusSummaryDto>,
    path: List<LatLng>? = null
)

/**
 * Custom bus marker for map with Material 3 styling
 */
@Composable
fun BusMarker(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    heading: Float = 0f,
    onClick: () -> Unit = {}
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.4f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "bus_marker_scale"
    )
    val rotation by animateFloatAsState(
        targetValue = heading,
        animationSpec = tween(durationMillis = 500),
        label = "bus_marker_rotation"
    )
    val bgColor = if (isSelected) Color(0xFF6200EE) else Color.White
    val iconColor = if (isSelected) Color.White else Color(0xFF6200EE)

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.Black.copy(alpha = 0.18f), radius = size.minDimension / 2,
                center = Offset(size.width / 2, size.height / 2 + 3.dp.toPx()))
            drawCircle(color = bgColor, radius = size.minDimension / 2)
            if (!isSelected) {
                drawCircle(color = Color(0xFF6200EE), radius = size.minDimension / 2,
                    style = Stroke(width = 2.5.dp.toPx()))
            }
        }
        Image(
            painter = painterResource(Res.drawable.ic_bus_top),
            contentDescription = null,
            modifier = Modifier.size(28.dp).rotate(rotation),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconColor)
        )
    }
}

/**
 * Custom stop marker - Redesigned to stand out as a transport hub
 * Using a "Badge" style that looks like official transport signage.
 */
@Composable
fun StopMarker(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.5f else 1.1f,
        animationSpec = tween(durationMillis = 300),
        label = "stop_marker_scale"
    )
    
    val primaryColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    
    Box(
        modifier = modifier
            .size(52.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Marcador tipo "Totem/Parada"
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            // Sombra
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = 8.dp.toPx(),
                center = Offset(centerX, size.height - 4.dp.toPx())
            )
            
            // Cuerpo cuadrado redondeado (parece señal de bus)
            val rectWidth = 32.dp.toPx()
            val rectHeight = 32.dp.toPx()
            val cornerRadius = 6.dp.toPx()
            
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(centerX - rectWidth / 2, centerY - rectHeight / 2 - 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )
            
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(centerX - rectWidth / 2, centerY - rectHeight / 2 - 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 3.dp.toPx())
            )

            // El palo de la señal
            val stemWidth = 4.dp.toPx()
            drawRect(
                color = primaryColor,
                topLeft = Offset(centerX - stemWidth / 2, centerY + 10.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(stemWidth, 8.dp.toPx())
            )
        }

        // Icono de bus
        Icon(
            imageVector = Icons.Default.DirectionsBus,
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .offset(y = (-4).dp),
            tint = primaryColor
        )
    }
}

/**
 * User location marker - High precision style
 */
@Composable
fun UserLocationMarker(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Onda expansiva de precisión
        Box(
            modifier = Modifier
                .size(16.dp)
                .scale(pulseScale)
                .background(Color(0xFF2196F3).copy(alpha = pulseAlpha), CircleShape)
        )
        
        // Punto central con borde de alta visibilidad
        Surface(
            modifier = Modifier.size(14.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .background(Color(0xFF2196F3), CircleShape)
            )
        }
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
