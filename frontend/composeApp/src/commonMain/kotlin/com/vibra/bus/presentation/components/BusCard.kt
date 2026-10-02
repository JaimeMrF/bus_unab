package com.vibra.bus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.OccupancyDto
import com.vibra.bus.presentation.theme.AppShape
import com.vibra.bus.presentation.theme.AppThemeUtils
import com.vibra.bus.presentation.theme.ensureContrast

/** Fila de bus: icono tonal, nombre/placa, estado (texto + color) y ocupación. ≥64dp de alto táctil. */
@Composable
fun BusCard(
    bus: BusSummaryDto,
    occupancy: OccupancyDto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFull = occupancy?.level == "full"
    val isAvailable = occupancy != null
    val statusLabel = when {
        isFull -> "Lleno"
        isAvailable -> "Disponible"
        else -> "Próximo"
    }
    val tone = when {
        isFull -> PillTone.Error
        isAvailable -> PillTone.Success
        else -> PillTone.Warning
    }
    val occupancyColor = occupancy?.level?.let { AppThemeUtils.occupancyColor(it) }
        ?: MaterialTheme.colorScheme.outline

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${bus.name}, placa ${bus.plate}, $statusLabel" +
                    (occupancy?.let { ", ocupación ${it.percentage.toInt()} por ciento" } ?: "")
            },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsBus,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = bus.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = bus.plate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusPill(text = statusLabel, tone = tone, showDot = true)
            }

            if (occupancy != null) {
                OccupancyBadge(
                    level = occupancy.level,
                    percentage = occupancy.percentage,
                    color = occupancyColor,
                )
                Spacer(Modifier.width(4.dp))
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun OccupancyBadge(level: String, percentage: Float, color: Color) {
    val surface = MaterialTheme.colorScheme.surface
    val container = lerp(color, surface, 0.86f)
    val content = ensureContrast(color, container)
    Column(
        modifier = Modifier
            .clip(AppShape.Chip)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.titleSmall,
            color = content,
        )
        Text(
            text = level.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}
