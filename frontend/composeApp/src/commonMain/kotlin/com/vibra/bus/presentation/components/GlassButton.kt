package com.vibra.bus.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.bus.presentation.theme.VibraBusShapes
import com.vibra.bus.presentation.theme.VibraBusThemeUtils

/**
 * Glassmorphism button with Material 3 design and optimized performance
 * Features linear gradients, blur effects, and smooth animations
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    glassType: GlassType = GlassType.PRIMARY
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedScale by animateFloatAsState(
        targetValue = if (interactionSource.collectIsPressedAsState().value) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "button_scale"
    )

    val (glassSurface, glassBorder, glassHighlight) = VibraBusThemeUtils.glassColors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(pressedScale)
            .shadow(
                elevation = 8.dp,
                shape = VibraBusShapes.ButtonPrimary,
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .clip(VibraBusShapes.ButtonPrimary)
            .drawBehind {
                drawGlassmorphismBackground(
                    glassType = glassType,
                    surfaceColor = glassSurface,
                    borderColor = glassBorder,
                    highlightColor = glassHighlight
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(glassHighlight, glassBorder, glassHighlight),
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                ),
                shape = VibraBusShapes.ButtonPrimary
            )
            .clickable(
                enabled = enabled && !loading,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(
                    color = MaterialTheme.colorScheme.primary,
                    radius = 26.dp
                )
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.invoke()
                if (icon != null) Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Primary glass button — usa el composable Button de M3.
 * NO acepta icon ni glassType porque Button de M3 no los soporta directamente.
 * Si necesitas icon o glass personalizado usa GlassButton en su lugar.
 */
@Composable
fun PrimaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    glassType: GlassType = GlassType.PRIMARY

) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedScale by animateFloatAsState(
        targetValue = if (interactionSource.collectIsPressedAsState().value) 0.96f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "primary_button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(pressedScale),
        enabled = enabled && !loading,
        shape = VibraBusShapes.ButtonPrimary,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            focusedElevation = 8.dp,
            hoveredElevation = 8.dp,
            disabledElevation = 0.dp
        ),
        interactionSource = interactionSource
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Secondary glass button con glassmorphism.
 * Parámetros opcionales: icon, showRecommendedBadge, glassType.
 */
@Composable
fun SecondaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    glassType: GlassType = GlassType.SECONDARY,
    showRecommendedBadge: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedScale by animateFloatAsState(
        targetValue = if (interactionSource.collectIsPressedAsState().value) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "secondary_button_scale"
    )

    val (glassSurface, glassBorder, glassHighlight) = VibraBusThemeUtils.glassColors()

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(54.dp)
                .scale(pressedScale)
                .shadow(
                    elevation = 4.dp,
                    shape = VibraBusShapes.ButtonSecondary,
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(VibraBusShapes.ButtonSecondary)
                .drawBehind {
                    drawGlassmorphismBackground(
                        glassType = glassType,
                        surfaceColor = glassSurface,
                        borderColor = glassBorder,
                        highlightColor = glassHighlight
                    )
                }
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glassHighlight,
                            glassBorder.copy(alpha = 0.4f),
                            glassHighlight
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    ),
                    shape = VibraBusShapes.ButtonSecondary
                )
                .clickable(
                    enabled = enabled && !loading,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = ripple(
                        color = MaterialTheme.colorScheme.secondary,
                        radius = 27.dp
                    )
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon?.invoke()
                    if (icon != null) Spacer(Modifier.width(8.dp))
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (showRecommendedBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 8.dp)
                    .clip(VibraBusShapes.StatusBadge)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = VibraBusShapes.StatusBadge
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Recomendado",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Glass effect types for different visual styles
 */
enum class GlassType {
    PRIMARY,    // Standard glass with brand colors
    SECONDARY,  // Subtle glass with neutral colors
    TERTIARY,   // Dark glass for high contrast
    MAP         // Optimized for map overlays
}

/**
 * Custom draw function for glassmorphism background
 */
private fun DrawScope.drawGlassmorphismBackground(
    glassType: GlassType,
    surfaceColor: Color,
    borderColor: Color,
    highlightColor: Color
) {
    val gradient = when (glassType) {
        GlassType.PRIMARY -> Brush.linearGradient(
            colors = listOf(surfaceColor, borderColor),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
        GlassType.SECONDARY -> Brush.radialGradient(
            colors = listOf(surfaceColor, borderColor.copy(alpha = 0.5f)),
            center = Offset(100f, 30f),
            radius = 80f
        )
        GlassType.TERTIARY -> Brush.linearGradient(
            colors = listOf(surfaceColor.copy(alpha = 0.8f), borderColor.copy(alpha = 0.6f)),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
        GlassType.MAP -> Brush.radialGradient(
            colors = listOf(surfaceColor, Color.Transparent),
            center = Offset(100f, 50f),
            radius = 60f
        )
    }

    drawRect(gradient)

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                highlightColor.copy(alpha = 0.3f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, size.height * 0.3f)
        )
    )
}