package com.vibra.bus.presentation.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import com.vibra.bus.presentation.motion.LocalMotion
import com.vibra.bus.presentation.motion.pressScale
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.theme.Spacing
import com.vibra.bus.presentation.theme.appColors

/** Tarjeta base del sistema: superficie tonal con borde fino (sin sombras pesadas). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    if (onClick != null) {
        val source = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.pressScale(source, 0.985f),
            shape = AppShape.Card,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
            interactionSource = source,
        ) { Column(content = content) }
    } else {
        Surface(
            modifier = modifier,
            shape = AppShape.Card,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) { Column(content = content) }
    }
}

/** Panel flotante sobre el mapa: vidrio sutil (superficie translúcida + borde) con sombra suave. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val app = MaterialTheme.appColors
    Surface(
        modifier = modifier.shadow(12.dp, AppShape.CardLarge, clip = false),
        shape = AppShape.CardLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, app.glassBorder),
    ) { Column(content = content) }
}

enum class PillTone { Neutral, Success, Warning, Error, Brand }

/** Etiqueta de estado: color + texto (nunca solo color) y punto opcional para "en vivo". */
@Composable
fun StatusPill(
    text: String,
    tone: PillTone = PillTone.Neutral,
    showDot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val app = MaterialTheme.appColors
    val (container, content) = when (tone) {
        PillTone.Neutral -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
        PillTone.Brand -> scheme.primaryContainer to scheme.onPrimaryContainer
        PillTone.Success -> toneOf(app.success, scheme.surface)
        PillTone.Warning -> toneOf(app.warning, scheme.surface)
        PillTone.Error -> scheme.errorContainer to scheme.onErrorContainer
    }
    Surface(modifier = modifier, shape = AppShape.StatusBadge, color = container) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showDot) {
                PulseDot(color = content)
                Spacer(Modifier.width(6.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

/** Contenedor tonal + texto legible (AA) para un color semántico. */
@Composable
private fun toneOf(base: Color, surface: Color): Pair<Color, Color> {
    val container = androidx.compose.ui.graphics.lerp(base, surface, 0.84f)
    return container to com.vibra.bus.presentation.theme.ensureContrast(base, container)
}

/** Encabezado de sección/pantalla con semántica de heading para lectores de pantalla. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** Punto de estado "en vivo": pulsa suavemente solo con movimiento ambiental; si no, queda fijo. */
@Composable
private fun PulseDot(color: Color) {
    val env = LocalMotion.current
    val pulse = if (env.ambient) {
        rememberInfiniteTransition(label = "dot").animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "dot_alpha",
        )
    } else null
    Box(
        Modifier
            .size(6.dp)
            .graphicsLayer { alpha = pulse?.value ?: 1f }
            .background(color, CircleShape),
    )
}
