package com.vibra.bus.presentation.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.bus.data.model.BusSummaryDto
import com.vibra.bus.data.model.StopWithPivotDto
import com.vibra.bus.presentation.theme.VibraBusShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailBottomSheet(
    bus: BusSummaryDto,
    stops: List<StopWithPivotDto>,
    isLoadingStops: Boolean = false,
    onDismiss: () -> Unit,
    onRequestBus: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = VibraBusShapes.BottomSheet,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            // Header
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment    = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text       = bus.name,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text     = "Placa: ${bus.plate}",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (stops.isNotEmpty()) {
                val totalMinutes = stops.maxOfOrNull { it.pivot.estimatedMinutes } ?: 0
                Text(
                    text     = "Duración total: $totalMinutes min",
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text       = "Paradas",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))

            if (isLoadingStops) {
                repeat(3) { ShimmerBox(height = 56.dp) }
            } else {
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    itemsIndexed(stops) { _, stop ->
                        StopRow(order = stop.pivot.order, stop = stop)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick  = onRequestBus,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    "Solicitar bus en esta ruta",
                    color      = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StopRow(order: Int, stop: StopWithPivotDto) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape    = RoundedCornerShape(8.dp),
            color    = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text       = "$order",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stop.name,    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = stop.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text     = "~${stop.pivot.estimatedMinutes} min",
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.primary,
        )
    }
}
